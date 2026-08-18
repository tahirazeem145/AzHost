# AZHost — Personal Deployment & Hosting Platform

> **Create → Select Project → Deploy → Live**

AZHost is a developer-friendly alternative to platforms such as Vercel and Netlify. It hides complicated cloud infrastructure terminology from normal users while providing a clean, powerful deployment architecture underneath.

---

## Current Status:
- [x] **Phase 1 — Foundation & Architecture**: Micro-service layout, Spring Boot backend, Vite React frontend, Docker Compose setup, Nginx reverse proxy, health check polling.
- [x] **Phase 2 — Project Management**: Full project lifecycle (Create, List, Search, View Details, Edit, Delete) with PostgreSQL relational persistence, Flyway migrations, and REST APIs.
- [x] **Phase 3 — Project Detection & Analysis**: Safe, non-executing metadata inspection engine detecting framework, build tool, package manager, Node version, commands, and output directories.
- [ ] **Phase 4 — Environment Variables & Source Code Integration**: GitHub OAuth & project repository cloning.


### Core Architecture

```text
  React 18 + Vite + TypeScript Frontend
                    ↓ (REST API /api/*)
       Spring Boot 3 (Java 17) Backend
                    ↓ (JDBC JPA)
            PostgreSQL 15 Database
```

All platform components are containerized and orchestrated via **Docker Compose**.

---

## Directory Structure

```text
AZHost/
│
├── frontend/                   # React 18 + Vite + TypeScript + Tailwind CSS
│   ├── src/
│   │   ├── components/         # Sidebar, Header, StatCard, EmptyState, NewProjectModal
│   │   ├── context/            # BackendStatusContext (real-time health polling)
│   │   ├── layouts/            # DashboardLayout
│   │   ├── pages/              # Dashboard, Projects, Deployments, LiveSites, Settings
│   │   ├── services/           # api.ts (Health & Info API client)
│   │   └── types/              # TypeScript interfaces
│   ├── package.json
│   ├── vite.config.ts
│   └── Dockerfile              # Multi-stage Nginx build for static React bundle
│
├── backend/                    # Spring Boot 3 REST API Backend
│   ├── src/
│   │   ├── main/java/com/azhost/
│   │   │   ├── AZHostApplication.java
│   │   │   ├── config/         # CorsConfig
│   │   │   ├── controller/     # HealthController, InfoController
│   │   │   ├── dto/            # HealthResponseDto, InfoResponseDto, ErrorResponseDto
│   │   │   ├── entity/         # User JPA entity (UUID, email, passwordHash)
│   │   │   ├── exception/      # GlobalExceptionHandler
│   │   │   ├── repository/     # UserRepository
│   │   │   ├── security/       # WebSecurityConfig foundation
│   │   │   └── service/        # SystemInfoService
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-local.yml
│   ├── pom.xml
│   └── Dockerfile              # Multi-stage Maven + Java 17 build
│
├── infrastructure/
│   ├── docker/postgres/init.sql# PostgreSQL initial database schema
│   └── nginx/                  # Nginx proxy configuration & documentation
│
├── docs/                       # Developer & Product Documentation
│   ├── architecture.md         # Modular system architecture
│   ├── database-architecture.md# Entity relationship model & future schema roadmap
│   ├── development-guide.md    # Local setup, execution & testing guide
│   └── roadmap.md              # 10-Phase master development roadmap
│
├── docker-compose.yml          # Container orchestration (PostgreSQL + Backend + Frontend)
├── .env.example                # Environment variable configuration template
├── .gitignore                  # Multi-tier ignore rules
└── README.md                   # Project overview & quickstart
```

---

## Prerequisites

Before running AZHost locally, ensure you have installed:

- **Node.js**: `v18.0+` or `v20.0+`
- **Java JDK**: JDK 17+
- **Apache Maven**: `v3.8+`
- **Docker Desktop** (or Docker Engine + Compose)
- **Git**

---

## Quickstart — Running Locally

### Option 1: Running with Docker Compose (Recommended)

To build and start all AZHost services (Database, Backend, Frontend):

```bash
docker compose up --build
```

#### Services Started:
1. **`postgres`**: PostgreSQL 15 database running on `localhost:5432` with persistent data storage (`postgres_data` volume).
2. **`backend`**: Spring Boot 3 REST API running on `http://localhost:8080`.
3. **`frontend`**: Nginx web server rendering the React dashboard on `http://localhost:3000`.

---

### Option 2: Running Services Individually (Local Development)

#### 1. Configure Environment
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```

#### 2. Start Backend Service
Set `JAVA_HOME` to Java 17+ if required:
```bash
$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr"
cd backend
mvn spring-boot:run
```
Backend will start on `http://localhost:8080`.

#### 3. Start Frontend App
In another terminal:
```bash
cd frontend
npm install
npm run dev
```
Vite dev server will start on `http://localhost:5173`.

---

## Verified REST API Endpoints

### Health Check Endpoint
```bash
GET /api/health
```
**Response:**
```json
{
  "status": "UP",
  "service": "AZHost"
}
```

### Application Info Endpoint
```bash
GET /api/info
```
**Response:**
```json
{
  "name": "AZHost",
  "version": "0.1.0",
  "phase": "Phase 1",
  "status": "development"
}
```

---

## Running Unit Tests

### Backend Unit Tests
Run context, controller MockMvc, and JPA repository tests:
```bash
cd backend
mvn clean test
```

### Frontend Build Test
Verify TypeScript compilation and Vite build bundle:
```bash
cd frontend
npm run build
```

---

## Documentation Links

- [System Architecture](docs/architecture.md)
- [Database Architecture & Entity Roadmap](docs/database-architecture.md)
- [Development Guide](docs/development-guide.md)
- [Master 10-Phase Product Roadmap](docs/roadmap.md)
