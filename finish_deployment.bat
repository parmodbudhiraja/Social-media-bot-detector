@echo off
echo --- Step 1: Login to Railway ---
call railway login --browserless

echo --- Step 2: Updating ML Service ---
cd ml-service
call railway variables set RABBITMQ_HOST=rabbitmq.railway.internal RABBITMQ_USER=admin RABBITMQ_PASS=password
call railway up --detach
cd ..

echo --- Step 3: Updating Backend Service ---
cd backend
call railway variables set SPRING_RABBITMQ_HOST=rabbitmq.railway.internal ML_SERVICE_URL=http://ml-service.railway.internal:8000 SPRING_DATASOURCE_URL=jdbc:postgresql://postgres.railway.internal:5432/jobsdb SPRING_RABBITMQ_USERNAME=admin SPRING_RABBITMQ_PASSWORD=password SPRING_DATASOURCE_USERNAME=postgres SPRING_DATASOURCE_PASSWORD=password APIFY_TOKEN=%APIFY_TOKEN%
call railway up --detach
cd ..

echo --- Step 4: Updating Frontend Service ---
cd frontend
call railway up --detach
cd ..

echo --- DEPLOYMENT TRIGGERED! ---
echo Wait 2-3 minutes for Railway to finish building, then visit:
echo https://authentic-success-production-7744.up.railway.app
pause

