import pika
import json
import os
import torch
import joblib
import pandas as pd
import numpy as np
import logging
from main import LSTMModel
from feature_extraction import extract_profile_features, extract_behavioral_features

# Configure Logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Configuration
RABBITMQ_HOST = os.getenv('RABBITMQ_HOST', 'localhost')
EXCHANGE_NAME = "instagram.analysis.exchange"
INFERENCE_QUEUE = "ml.inference.queue"
RESULTS_QUEUE = "ml.results.queue"
ROUTING_KEY_RESULTS = "ml.results"

# Load Models
rf_model = None
lstm_model = None

def load_models():
    global rf_model, lstm_model
    logger.info("Consumer: Starting model load sequence")
    try:
        if os.path.exists('models/random_forest.pkl'):
            rf_model = joblib.load('models/random_forest.pkl')
            logger.info("Consumer: Successfully loaded RF Model")
        else:
            logger.warning("Consumer: RF Model file missing")
            
        if os.path.exists('models/lstm_weights.pth'):
            lstm_model = LSTMModel(input_size=5, hidden_size=16, num_layers=1, num_classes=2)
            lstm_model.load_state_dict(torch.load('models/lstm_weights.pth'))
            lstm_model.eval()
            logger.info("Consumer: Successfully loaded LSTM Model")
        else:
            logger.warning("Consumer: LSTM Model weights missing")
    except Exception as e:
        logger.error(f"Consumer Error: Models could not be loaded: {str(e)}")

def process_message(ch, method, properties, body):
    try:
        data = json.loads(body)
        job_id = data.get('job_id')
        items = data.get('items', [])
        
        logger.info(f"Consumer: Processing Job {job_id} ({len(items)} items)")
        
        if not items:
            logger.warn(f"Consumer: Job {job_id} received with empty items list")
            ch.basic_ack(delivery_tag=method.delivery_tag)
            return

        # 1. Profile RF Predictions
        profile_features = [extract_profile_features(item) for item in items]
        rf_df = pd.DataFrame(profile_features)
        
        # Ensure correct columns
        features_columns = [
            "profile_pic", "nums/length_username", "fullname_words", 
            "nums/length_fullname", "name==username", "description_length", 
            "external_URL", "private", "posts", "followers", "following"
        ]
        for col in features_columns:
            if col not in rf_df.columns:
                rf_df[col] = 0
        
        x_preds = [0] * len(items)
        if rf_model:
            x_preds = rf_model.predict(rf_df[features_columns]).tolist()
        else:
            logger.info(f"Consumer: Job {job_id} Model RF missing, providing defaults")
            
        # 2. Behavioral LSTM Predictions
        y_preds = [0] * len(items)
        if lstm_model:
            seq_list = [extract_behavioral_features(item, seq_length=10) for item in items]
            seq_tensors = torch.Tensor(np.array(seq_list))
            with torch.no_grad():
                outputs = lstm_model(seq_tensors)
                _, predicted = torch.max(outputs.data, 1)
                y_preds = predicted.tolist()
        else:
             logger.info(f"Consumer: Job {job_id} Model LSTM missing, providing defaults")

        # Extract Usernames for joined report
        usernames = [str(item.get("profile", {}).get("username", "unknown")) for item in items]

        # Send results back
        result_payload = {
            "job_id": job_id,
            "usernames": usernames,
            "x_predictions": x_preds,
            "y_predictions": y_preds
        }
        
        ch.basic_publish(
            exchange=EXCHANGE_NAME,
            routing_key=ROUTING_KEY_RESULTS,
            body=json.dumps(result_payload)
        )
        logger.info(f"Consumer: Completed inference for Job {job_id} and published to {RESULTS_QUEUE}")
        ch.basic_ack(delivery_tag=method.delivery_tag)
        
    except Exception as e:
        logger.error(f"Consumer Error: Failed to process message: {str(e)}")
        # Reject and requeue or discard depending on error
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

def main():
    load_models()
    
    max_retries = 30
    connection = None
    for attempt in range(max_retries):
        try:
            connection = pika.BlockingConnection(
                pika.ConnectionParameters(
                    host=RABBITMQ_HOST,
                    credentials=pika.PlainCredentials(
                        os.getenv('RABBITMQ_USER', 'admin'),
                        os.getenv('RABBITMQ_PASS', 'password')
                    )
                )
            )
            logger.info("Consumer: RabbitMQ connection established")
            break
        except pika.exceptions.AMQPConnectionError:
            logger.info(f"Consumer: RabbitMQ not ready, retrying ({attempt+1}/{max_retries})...")
            time.sleep(2)
    else:
        logger.critical("Consumer: Failed to connect to RabbitMQ after retries. Exiting.")
        return
    
    channel = connection.channel()
    
    channel.queue_declare(queue=INFERENCE_QUEUE, durable=True)
    channel.queue_declare(queue=RESULTS_QUEUE, durable=True)
    
    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(queue=INFERENCE_QUEUE, on_message_callback=process_message)
    
    logger.info(" [*] Consumer: Waiting for inference requests. To exit press CTRL+C")
    channel.start_consuming()

if __name__ == "__main__":
    main()
