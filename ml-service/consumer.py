import pika
import json
import os
import torch
import joblib
import pandas as pd
import numpy as np
from main import LSTMModel
from feature_extraction import extract_profile_features, extract_behavioral_features

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
    try:
        if os.path.exists('models/random_forest.pkl'):
            rf_model = joblib.load('models/random_forest.pkl')
            print("Consumer: Loaded RF Model")
        if os.path.exists('models/lstm_weights.pth'):
            lstm_model = LSTMModel(input_size=5, hidden_size=16, num_layers=1, num_classes=2)
            lstm_model.load_state_dict(torch.load('models/lstm_weights.pth'))
            lstm_model.eval()
            print("Consumer: Loaded LSTM Model")
    except Exception as e:
        print(f"Consumer Warning: Models could not be loaded: {e}")

def process_message(ch, method, properties, body):
    try:
        data = json.loads(body)
        job_id = data.get('job_id')
        items = data.get('items', [])
        
        if not items:
            print(f"Empty items for job {job_id}")
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
            
        # 2. Behavioral LSTM Predictions
        y_preds = [0] * len(items)
        if lstm_model:
            seq_list = [extract_behavioral_features(item, seq_length=10) for item in items]
            seq_tensors = torch.Tensor(np.array(seq_list))
            with torch.no_grad():
                outputs = lstm_model(seq_tensors)
                _, predicted = torch.max(outputs.data, 1)
                y_preds = predicted.tolist()

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
        print(f"Processed job {job_id} and sent results")
        ch.basic_ack(delivery_tag=method.delivery_tag)
        
    except Exception as e:
        print(f"Error processing message: {e}")
        # Reject and requeue or discard depending on error
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

def main():
    load_models()
    
    import time
    max_retries = 30
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
            break
        except pika.exceptions.AMQPConnectionError:
            print(f"RabbitMQ not ready, retrying ({attempt+1}/{max_retries})...")
            time.sleep(2)
    else:
        print("Failed to connect to RabbitMQ after retries. Exiting.")
        return
    
    channel = connection.channel()
    
    channel.queue_declare(queue=INFERENCE_QUEUE, durable=True)
    channel.queue_declare(queue=RESULTS_QUEUE, durable=True)
    
    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(queue=INFERENCE_QUEUE, on_message_callback=process_message)
    
    print(" [*] Waiting for inference requests. To exit press CTRL+C")
    channel.start_consuming()

if __name__ == "__main__":
    main()
