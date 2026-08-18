# AZHost Developer Guide

This guide provides setup, local execution, and testing procedures for developers working on the **AZHost** platform.

---

## 1. Prerequisites

Ensure you have the following installed on your machine:

- **Node.js**: `v18.0+` or `v20.0+`
- **npm**: `v9.0+` or `v10.0+`
- **Java JDK**: OpenJDK 17+ (or JDK 25 with Java 17 target compatibility)
- **Apache Maven**: `v3.8+`
- **Docker Desktop**: (Optional for non-containerized local dev, required for full environment verification)

---

## 2. Environment Setup

Copy `.env.example` to `.env` in the project root:

```bash
cp .env.example .env
```

Ensure environment variables are configured:

```env
POSTGRES_DB=azhost
POSTGRES_USER=azhost_user
POSTGRES_PASSWORD=azhost_password
DATABASE_URL=jdbc:postgresql://localhost:5432/azhost
SERVER_PORT=8080
VITE_API_URL=http://localhost:8080
```

---

## 3. Running Locally (Without Docker)

### 3.1 Backend Service (Spring Boot)
Ensure Java 17+ and Maven are in your environment path:

```bash
# Set JAVA_HOME if required
$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr"

# Navigate to backend directory
cd backend

# Execute unit tests
mvn clean test

# Run application locally
mvn spring-boot:run
```
The backend REST API will start on `http://localhost:8080`.

Verify health endpoint:
```bash
curl http://localhost:8080/api/health
```

### 3.2 Frontend Application (React + Vite)
In a separate terminal:

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start Vite dev server
npm run dev
```
The dashboard UI will open on `http://localhost:5173`.

---

## 4. Running via Docker Compose

To launch the complete containerized stack (PostgreSQL + Spring Boot + React/Nginx):

```bash
docker compose up --build
```

Services exposed:
- **Frontend Dashboard**: `http://localhost:3000` (Nginx container)
- **Spring Boot Backend**: `http://localhost:8080`
- **PostgreSQL Database**: `localhost:5432`

To stop all services:
```bash
docker compose down -v
```

---

## 5. Running Automated Unit Tests

### Backend Tests:
```bash
cd backend
mvn test
```

Tests run against an in-memory H2 database under the `test` profile:
- `AZHostApplicationTests`: Verifies Spring Boot context initialization.
- `HealthControllerTest`: MockMvc assertion of `GET /api/health`.
- `InfoControllerTest`: MockMvc assertion of `GET /api/info`.
- `UserRepositoryTest`: DataJpaTest verifying User entity persistence and UUID generation.

### Frontend Build Verification:
```bash
cd frontend
npm run build
```
