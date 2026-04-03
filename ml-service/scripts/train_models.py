import pandas as pd
import numpy as np
import os
import joblib
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score
import torch
import torch.nn as nn
import torch.optim as optim

def train_random_forest(df):
    print("Training Random Forest on Kaggle Profile Metrics...")
    
    # 11 features matching Kaggle dataset
    features = [
        "profile_pic", "nums/length_username", "fullname_words", 
        "nums/length_fullname", "name==username", "description_length", 
        "external_URL", "private", "posts", "followers", "following"
    ]
    
    # Check if we have the right features
    missing = [f for f in features if f not in df.columns]
    if missing:
        print(f"Warning: Missing features {missing}. Training might fail.")
    
    X = df[features]
    y = df['is_fake']
    
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.5, random_state=42)
    
    rf = RandomForestClassifier(n_estimators=100, max_depth=15, random_state=42)
    rf.fit(X_train, y_train)
    
    preds = rf.predict(X_test)
    accuracy = accuracy_score(y_test, preds)
    print(f"Random Forest Accuracy (Test Split): {accuracy:.4f}")
    
    os.makedirs('models', exist_ok=True)
    joblib.dump(rf, 'models/random_forest.pkl')
    
    import json
    with open('models/rf_accuracy.json', 'w') as f:
        json.dump({"accuracy": float(accuracy)}, f)
        
    print("Saved Random Forest model and accuracy metrics.")

class LSTMModel(nn.Module):
    def __init__(self, input_size, hidden_size, num_layers, num_classes):
        super(LSTMModel, self).__init__()
        self.hidden_size = hidden_size
        self.num_layers = num_layers
        self.lstm = nn.LSTM(input_size, hidden_size, num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_size, num_classes)
    
    def forward(self, x):
        h0 = torch.zeros(self.num_layers, x.size(0), self.hidden_size)
        c0 = torch.zeros(self.num_layers, x.size(0), self.hidden_size)
        out, _ = self.lstm(x, (h0, c0))
        out = self.fc(out[:, -1, :])
        return out

import json

def train_lstm(df):
    print(f"Training LSTM for Sequence Evaluation on {len(df)} rows...")
    
    # Correct columns: 'userid(username)', 'List of Comment-level data', 'List of Like-level data', 'Is a bot?(bool)'
    seq_length = 10
    num_features = 5
    
    X_seq = np.zeros((len(df), seq_length, num_features))
    y_seq = df['Is a bot?(bool)'].map({True: 1, False: 0}).values
    
    print("Parsing JSON lists and extracting features...")
    for idx, row in df.iterrows():
        try:
            comments = json.loads(row['List of Comment-level data'])
            # Sort comments by timestamp
            comments = sorted(comments, key=lambda x: x.get('timestamp', 0))
            
            last_t = 0
            for i in range(seq_length):
                if i < len(comments):
                    c = comments[i]
                    t = c.get('timestamp', 0)
                    gap = t - last_t if last_t > 0 else 0
                    last_t = t
                    
                    X_seq[idx, i, 0] = len(c.get('text', ''))
                    X_seq[idx, i, 1] = gap
                    X_seq[idx, i, 2] = c.get('likes', 0)
                    X_seq[idx, i, 3] = 1 # is a comment
                    X_seq[idx, i, 4] = 0 # pad
                else:
                    # Padding
                    X_seq[idx, i, :] = 0
        except Exception as e:
            continue
            
    X_train, X_test, y_train, y_test = train_test_split(X_seq, y_seq, test_size=0.5, random_state=42)
    
    tensor_X_train = torch.Tensor(X_train)
    tensor_y_train = torch.LongTensor(y_train)
    tensor_X_test = torch.Tensor(X_test)
    tensor_y_test = torch.LongTensor(y_test)
    
    model = LSTMModel(input_size=num_features, hidden_size=16, num_layers=1, num_classes=2)
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.Adam(model.parameters(), lr=0.01)
    
    epochs = 15
    print("Starting training loop...")
    for epoch in range(epochs):
        model.train()
        optimizer.zero_grad()
        outputs = model(tensor_X_train)
        loss = criterion(outputs, tensor_y_train)
        loss.backward()
        optimizer.step()
        if (epoch+1) % 5 == 0:
            print(f"  Epoch {epoch+1}/{epochs}, Loss: {loss.item():.4f}")
    
    model.eval()
    with torch.no_grad():
        test_outputs = model(tensor_X_test)
        _, predicted = torch.max(test_outputs.data, 1)
        accuracy = accuracy_score(y_test, predicted.numpy())
        
    print(f"LSTM Final Loss: {loss.item():.4f}")
    print(f"LSTM Accuracy (Test Split): {accuracy:.4f}")
    
    os.makedirs('models', exist_ok=True)
    torch.save(model.state_dict(), 'models/lstm_weights.pth')
    
    with open('models/lstm_accuracy.json', 'w') as f:
         json.dump({"accuracy": float(accuracy)}, f)
         
    print("Saved LSTM model and accuracy metrics.")

if __name__ == "__main__":
    if os.path.exists('data/kaggle_instagram_profiles.csv'):
        df_rf = pd.read_csv('data/kaggle_instagram_profiles.csv')
        train_random_forest(df_rf)
    else:
        print("Kaggle dataset not found. Please run download_kaggle_dataset.py first.")
        
    if os.path.exists('data/behavioral_amplified_v2.csv'):
        df_lstm = pd.read_csv('data/behavioral_amplified_v2.csv')
        train_lstm(df_lstm)
    else:
        print("Behavioral dataset not found. Please run behavioral scripts first.")
