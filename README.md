# Fake Engagement Detector

A full-stack application to automatically scrape, process, and classify the engagement of an Instagram post to identify fake interactions and bot accounts.The accuracy of the application is more than 90%.

---

## 🚀 Deployed Project
The application is live at:
**[https://authentic-success-production-7744.up.railway.app/](https://authentic-success-production-7744.up.railway.app/)**
Prefer running it in incognito mode.
---

## 📽️ Project Demo
Click to watch the demo of the deployed project:
[![Watch the Demo](https://img.shields.io/badge/Watch-Demo-red?style=for-the-badge&logo=youtube)](https://drive.google.com/file/d/1-Vf2Uekf4JjVTiFSaUQe37xoFctmjltg/view?usp=sharing)

---

## 🏗️ Architecture
- **Backend:** Spring Boot (Java 17) - Orchestrates the analysis pipeline, polls Apify for scraping data, manages job states in PostgreSQL, and dispatches tasks via RabbitMQ.
- **ML Service:** Python (FastAPI) - Performs inference on gathered data using optimized Random Forest and LSTM models.
- **Frontend:** React (Vite) - Material Design 3 (M3) compliant interface with real-time status tracking via Server-Sent Events (SSE).
- **Infrastructure:** Containerized environment using PostgreSQL for persistence and RabbitMQ for asynchronous processing.

---

## 💻 Local Development & Setup

Follow these steps to run the complete stack on your local machine using Docker Compose.

### 1. Prerequisites
- **Docker** and **Docker Compose** installed.
- **Apify API Token**: You'll need a free token to enable Instagram scraping. Get it at [Apify Console](https://console.apify.com/account/integrations).

### 2. Generate Environment Configuration
The application relies on environment variables to securely handle API tokens.
1. **Clone the repository:**
   ```bash
   git clone https://github.com/parmodbudhiraja/Social-media-bot-detector.git
   cd Social-media-bot-detector
   ```
2. **Setup the `.env` file:**
   Copy the provided example file:
   ```bash
   cp .env.example .env
   ```
3. **Configure your Secrets:**
   Open the `.env` file in your preferred editor and paste your Apify token:
   ```env
   APIFY_TOKEN=apify_api_your_unique_token_here
   ```

### 3. Build and Run the Stack
1. Ensure that Docker Desktop is running.
2. Initialize all services (PostgreSQL, RabbitMQ, ML Service, Backend, and Frontend) with a single command:
```bash
docker-compose up -d --build
```

### 4. Access the Application
Once the containers are running:
- **Frontend UI:** Open [http://localhost](http://localhost) (Port 80)
- **Backend API:** [http://localhost:8080/api/v1/jobs](http://localhost:8080/api/v1/jobs)
- **RabbitMQ Dashboard:** [http://localhost:15672](http://localhost:15672) (User: `admin`, Pass: `password`)

### 5. Stopping the Project
To stop and remove the containers:
```bash
docker-compose down
```

---

## 🛠️ Internal Metadata Archive
The repository includes a collection of sanitized metadata and logs for auditing and debugging:
- **`finish_deployment.bat`**: Deployment automation script.
- **`backend/backend_vars.json`**: Reference for internal service networking.
- **`git_log.txt`**: Project version history snapshot.

---
