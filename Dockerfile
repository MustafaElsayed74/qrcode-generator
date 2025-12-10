# syntax=docker/dockerfile:1

# Stage 1: Build Angular frontend
FROM node:20-alpine AS frontend
WORKDIR /workspace/qrcodegenerator

# Install dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy source and build
COPY . .
RUN npm run build

# Place built assets into Spring Boot static folder
RUN mkdir -p src/main/resources/static \
    && cp -r dist/qrcodegenerator/browser/* src/main/resources/static/


# Stage 2: Build Spring Boot jar
FROM maven:3.9.9-eclipse-temurin-21 AS backend
WORKDIR /app

COPY --from=frontend /workspace/qrcodegenerator /app
RUN chmod +x mvnw \
    && ./mvnw -DskipTests package


# Stage 3: Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy built jar
COPY --from=backend /app/target/qrcodegenerator-0.0.1-SNAPSHOT.jar /app/app.jar

# Render will provide PORT; app reads server.port=${PORT:9091}
ENV PORT=8080
EXPOSE 8080

# Use same working directory for uploads/pages so a Persistent Disk can mount at /app
CMD ["java","-jar","/app/app.jar"]

