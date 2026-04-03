# Instagram Engagement Analyzer (Bot Detector)

A full-stack application to automatically scrape, process, and classify the engagement of an Instagram post to identify fake interactions and bot accounts.

## Architecture
- **Backend:** Spring Boot (Java) handling orchestration, Apify polling, database state management, and RabbitMQ messaging.
- **ML Service:** Python microservice running FastAPI and Modal, using Random Forest and LSTM models.
- **Frontend:** React application built with Vite and Tailwind CSS.
- **Infrastructure:** Local PostgreSQL database and RabbitMQ message broker.
