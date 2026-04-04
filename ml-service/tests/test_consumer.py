import pytest
import json
import numpy as np
import sys
import os
from unittest.mock import MagicMock, patch

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import consumer

@pytest.fixture
def mock_channel():
    return MagicMock()

@pytest.fixture
def mock_method():
    method = MagicMock()
    method.delivery_tag = 1
    return method

def test_process_message_empty_items(mock_channel, mock_method):
    body = json.dumps({"job_id": "job-123", "items": []})
    
    consumer.process_message(mock_channel, mock_method, None, body)
    
    mock_channel.basic_ack.assert_called_once_with(delivery_tag=1)
    mock_channel.basic_publish.assert_not_called()

def test_process_message_without_models(mock_channel, mock_method):
    consumer.rf_model = None
    consumer.lstm_model = None
    
    body = json.dumps({
        "job_id": "job-123", 
        "items": [
            {"profile": {"username": "testuser"}}
        ]
    })
    
    consumer.process_message(mock_channel, mock_method, None, body)
    
    mock_channel.basic_ack.assert_called_once_with(delivery_tag=1)
    mock_channel.basic_publish.assert_called_once()
    
    call_args = mock_channel.basic_publish.call_args[1]
    assert call_args["exchange"] == "instagram.analysis.exchange"
    assert call_args["routing_key"] == "ml.results"
    
    payload = json.loads(call_args["body"])
    assert payload["job_id"] == "job-123"
    assert payload["usernames"] == ["testuser"]
    assert payload["x_predictions"] == [0]
    assert payload["y_predictions"] == [0]

def test_process_message_with_mock_models(mock_channel, mock_method):
    mock_rf = MagicMock()
    mock_rf.predict.return_value = np.array([1])
    
    mock_lstm = MagicMock()
    # Mock lstm returning a tensor (1, 2) where class 1 is highest
    import torch
    mock_lstm.return_value = torch.tensor([[0.1, 0.9]])
    
    consumer.rf_model = mock_rf
    consumer.lstm_model = mock_lstm
    
    body = json.dumps({
        "job_id": "job-123", 
        "items": [
            {
                "profile": {"username": "testuser"},
                "comments": [{"text": "Hello", "likes": 0, "timestamp": 123}]
            }
        ]
    })
    
    consumer.process_message(mock_channel, mock_method, None, body)
    
    mock_channel.basic_ack.assert_called_once_with(delivery_tag=1)
    
    call_args = mock_channel.basic_publish.call_args[1]
    payload = json.loads(call_args["body"])
    assert payload["x_predictions"] == [1]
    assert payload["y_predictions"] == [1]

def test_process_message_exception_handling(mock_channel, mock_method):
    body = "not valid json"
    
    consumer.process_message(mock_channel, mock_method, None, body)
    
    mock_channel.basic_nack.assert_called_once_with(delivery_tag=1, requeue=False)
