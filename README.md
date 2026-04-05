# Instagram Engagement Analyzer (Bot Detector)

A full-stack application to automatically scrape, process, and classify the engagement of an Instagram post to identify fake interactions and bot accounts.

## Architecture
- **Backend:** Spring Boot (Java) handling orchestration, Apify polling, database state management, and RabbitMQ messaging.
- **ML Service:** Python microservice running FastAPI and Modal, using Random Forest and LSTM models.
- **Frontend:** React application built with Vite and Tailwind CSS.
## Local Development

You can run the entire stack locally using Docker Compose.

### Prerequisites
- Docker and Docker Compose installed.

### Setup
1. **Clone the repository:**
   ```bash
   git clone https://github.com/parmodbudhiraja/Social-media-bot-detector.git
   cd Social-media-bot-detector
   ```

2. **Configure Environment:**
   Copy the example environment file and add your Apify token:
   ```bash
   cp .env.example .env
   # Edit .env and paste your APIFY_TOKEN
   ```

3. **Launch with Docker Compose:**
   ```bash
   docker-compose up -d --build
   ```

4. **Access the Application:**
   - **Frontend:** http://localhost:80
   - **Backend API:** http://localhost:8080/api/v1/jobs
   - **RabbitMQ UI:** http://localhost:15672 (admin/password)

---
