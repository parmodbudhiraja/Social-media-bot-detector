FROM python:3.10-slim

WORKDIR /app

# Install dependencies needed by CTGAN or other compiled modules
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu && \
    pip install --no-cache-dir -r requirements.txt

# Copy application and pre-trained models
COPY . .

EXPOSE 8000

# Start script runs both the FastAPI server and the RabbitMQ consumer
COPY start.sh .
RUN chmod +x start.sh
CMD ["./start.sh"]
