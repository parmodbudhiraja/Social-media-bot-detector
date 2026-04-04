from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any
import numpy as np
import os
import json
import joblib
import torch
from scripts.train_models import LSTMModel
import pandas as pd
import logging

# Configure Logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = FastAPI(title="Instagram Engagement ML Inference", version="1.0")

@app.get("/accuracy")
def get_accuracy():
    logger.info("Accuracy request received")
    results = {}
    try:
        if os.path.exists('models/rf_accuracy.json'):
            with open('models/rf_accuracy.json', 'r') as f:
                results['rf'] = json.load(f)
        if os.path.exists('models/lstm_accuracy.json'):
            with open('models/lstm_accuracy.json', 'r') as f:
                results['lstm'] = json.load(f)
        logger.debug(f"Returning accuracy results: {results}")
        return results
    except Exception as e:
        logger.error(f"Error fetching accuracy: {str(e)}")
        return {"error": str(e)}

class BatchRequest(BaseModel):
    job_id: str
    items: List[Dict[str, Any]]

class BatchResponse(BaseModel):
    job_id: str
    status: str
    x_predictions: List[int]
    y_predictions: List[int]

# We would load models in memory here
rf_model = None
lstm_model = None

@app.on_event("startup")
def load_models():
    global rf_model, lstm_model
    logger.info("ML API: Starting model load sequence")
    try:
        if os.path.exists('models/random_forest.pkl'):
            rf_model = joblib.load('models/random_forest.pkl')
            logger.info("ML API: Successfully loaded Random Forest Model")
        else:
            logger.warning("ML API: Random Forest Model file not found")
        
        if os.path.exists('models/lstm_weights.pth'):
            lstm_model = LSTMModel(input_size=5, hidden_size=16, num_layers=1, num_classes=2)
            lstm_model.load_state_dict(torch.load('models/lstm_weights.pth'))
            lstm_model.eval()
            logger.info("ML API: Successfully loaded LSTM Model")
        else:
            logger.warning("ML API: LSTM Model file not found")
    except Exception as e:
        logger.error(f"ML API ERROR: Models could not be loaded: {str(e)}")

from feature_extraction import extract_profile_features, extract_behavioral_features

@app.post("/predict/batch", response_model=BatchResponse)
def predict_batch(request: BatchRequest):
    logger.info(f"ML API: Received batch prediction for Job {request.job_id} ({len(request.items)} items)")
    if not request.items:
        logger.error(f"ML API: Job {request.job_id} items list is empty")
        raise HTTPException(status_code=400, detail="Items list cannot be empty")
        
    n_items = len(request.items)
    
    try:
        # 1. Feature Extraction for Random Forest (Model 1)
        profile_features_list = [extract_profile_features(item) for item in request.items]
        rf_features_df = pd.DataFrame(profile_features_list)
        
        x_preds = []
        if rf_model:
            # Match columns order from training
            features_columns = [
                "profile_pic", "nums/length_username", "fullname_words", 
                "nums/length_fullname", "name==username", "description_length", 
                "external_URL", "private", "posts", "followers", "following"
            ]
            # Ensure all columns exist 
            for col in features_columns:
                if col not in rf_features_df.columns:
                    rf_features_df[col] = 0
            
            X_rf = rf_features_df[features_columns]
            x_preds = rf_model.predict(X_rf).tolist()
            logger.debug(f"ML API: Job {request.job_id} Model 1 (RF) predictions complete")
        else:
            logger.warning(f"ML API: Job {request.job_id} Model 1 (RF) missing, falling back to mock")
            x_preds = [1 if np.random.rand() > 0.5 else 0 for _ in range(n_items)]
            
        # 2. Feature Extraction for LSTM (Model 2)
        y_preds = []
        if lstm_model:
            seq_list = [extract_behavioral_features(item, seq_length=10) for item in request.items]
            seq_tensors = torch.Tensor(np.array(seq_list))
            with torch.no_grad():
                outputs = lstm_model(seq_tensors)
                _, predicted = torch.max(outputs.data, 1)
                y_preds = predicted.tolist()
            logger.debug(f"ML API: Job {request.job_id} Model 2 (LSTM) predictions complete")
        else:
            logger.warning(f"ML API: Job {request.job_id} Model 2 (LSTM) missing, falling back to mock")
            y_preds = [1 if np.random.rand() > 0.7 else 0 for _ in range(n_items)]
            
        logger.info(f"ML API: Inference complete for Job {request.job_id}")
        return BatchResponse(
            job_id=request.job_id,
            status="SUCCESS",
            x_predictions=x_preds,
            y_predictions=y_preds
        )
    except Exception as e:
        logger.error(f"ML API ERROR: Job {request.job_id} inference failed: {str(e)}")
        import traceback
        logger.error(traceback.format_exc())
        raise HTTPException(status_code=500, detail=str(e))
