# LearnSmart - Adaptive Learning Platform (Backend)

This repository contains the microservices-based backend for the LearnSmart Adaptive Learning Platform. The system is designed to provide personalized learning paths, content generation via AI, and adaptive assessments.

## Architecture

The system follows a microservices architecture using Spring Boot (Java) and FastAPI (Python), orchestrated via Docker Compose.

### Services
*   **Gateway** (Spring Cloud Gateway): API Entry point (Port `8762`). Routes traffic to internal services.
*   **Eureka Server** (Spring Cloud Netflix): Service Discovery (Port `8761`).
*   **Profile Service**: Manages User Registration, Authentication (Stub), and Profiles (Port `8081`).
*   **Content Service**: Manages Educational Domains, Skills, and Content Items (Port `8082`).
*   **Planning Service**: Manages Learning Plans and Modules (Port `8083`).
*   **Assessment Service**: Handles Quizzes, Adaptive Questions, and Scoring (Port `8084`).
*   **Tracking Service**: Collects learning events and analytics (Port `8085`).
*   **AI Service**: Python/FastAPI service integrating OpenAI for content generation and adaptive logic (Port `8000`).

### Databases
Each microservice owns its isolated PostgreSQL database instance running in Docker.

## Prerequisites

*   **Docker** & **Docker Compose**
*   **Java 17+** (Optional, for local development)
*   **Maven** (Optional, build is handled via Docker)
*   **OpenAI API Key** (Required for AI features)

## Getting Started

### 1. Configuration
Create a `.env` file in `ai-service/` or set the environment variable in `docker-compose.yml`:
```bash
OPENAI_API_KEY=sk-your-key-here
```

### 2. Build and Run
The entire stack can be launched with a single command. The Dockerfile for each service handles the Maven build process (Multi-stage build).

```bash
docker compose up -d --build
```

Wait a few minutes for all services to register with Eureka. You can check the status at `http://localhost:8761`.

### 3. API Usage
All endpoints are accessible via the Gateway at `http://localhost:8762`.

#### Example Flow
1.  **Register User**:
    ```bash
    curl -X POST http://localhost:8762/profiles \
      -H "Content-Type: application/json" \
      -d '{"displayName": "Student", "email": "student@test.com", "password": "password"}'
    ```
2.  **Generate Plan**:
    ```bash
    curl -X POST http://localhost:8762/plans \
      -H "Content-Type: application/json" \
      -d '{"userId": "<UUID>", "goal": "Learn Java"}'
    ```

## Project Structure

```
backend/
├── ai-service/          # Python FastAPI (AI Logic)
├── assessment-service/  # Spring Boot (Exams & Grading)
├── back-end-eureka/     # Service Discovery
├── content-service/     # Spring Boot (Content Management)
├── gateway/             # API Gateway
├── planning-service/    # Spring Boot (Study Plans)
├── profile-service/     # Spring Boot (Users)
├── tracking-service/    # Spring Boot (Analytics)
└── docker-compose.yml   # Orchestration
```

## License
MIT
