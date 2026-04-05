FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
RUN apk add --no-cache ca-certificates
# Use the Nginx template system to dynamically set the port
COPY nginx.conf /etc/nginx/templates/default.conf.template

# Copy built assets to Nginx html folder
COPY --from=build /app/dist /usr/share/nginx/html

# Custom nginx config to handle React router if needed (optional)
# Using default but ensuring 80 is exposed
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
