#!/bin/bash



# Start the RabbitMQ consumer in the background



python consumer.py &







# Start the FastAPI server in the foreground



uvicorn main:app --host 0.0.0.0 --port 8000