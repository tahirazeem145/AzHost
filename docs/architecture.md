# AZHost System Architecture

AZHost is designed as a modular, cloud-agnostic deployment platform structured to scale from single-server VPS setups to distributed containerized infrastructure without requiring architectural rewrites.

---

## 1. Top-Level Architectural Overview

```text
  +-------------------------------------------------------------------+
  |                        AZHost Web Client                          |
  |             (React 18 + Vite + TypeScript + Tailwind)             |
  +-------------------------------------------------------------------+
                                    |
                                    | HTTP / REST API requests (/api/*)
                                    v
  +-------------------------------------------------------------------+
  |                         Nginx Gateway                             |
  |        (Reverse Proxy & Static Asset Server on Port 80)           |
  +-------------------------------------------------------------------+
                                    |
                                    v
  +-------------------------------------------------------------------+
  |                     Spring Boot 3 Backend                         |
  |        (Java 17, Spring Web, Security, Data JPA on Port 8080)     |
  +-------------------------------------------------------------------+
                                    |
                                    v
  +-------------------------------------------------------------------+
  |                      PostgreSQL 15 Database                       |
  |                   (Persistent Data Storage)                       |
  +-------------------------------------------------------------------+
```

---

## 2. Component Breakdown

### 2.1 Frontend Component Layer (`frontend/`)
- **Technology**: React 18, Vite, TypeScript, Tailwind CSS, Lucide Icons, React Router 6.
- **Responsibility**: Provides a developer-centric dashboard UI hiding infrastructure complexity while rendering live system diagnostics.
- **State Management & Polling**: Uses React Context (`BackendStatusContext`) to automatically poll `/api/health` and `/api/info` endpoints, providing real-time backend connectivity status indicators (`Backend ● Connected` / `Backend ● Offline`).

### 2.2 Backend Application Service (`backend/`)
- **Technology**: Java 17, Spring Boot 3.2, Spring Data JPA, Spring Security 6, PostgreSQL Driver, Validation.
- **Responsibility**:
  - **Controllers**: Stateless REST API controllers exposing `/api/health` and `/api/info`.
  - **Services**: Business logic encapsulation separating HTTP handling from domain workflows.
  - **Repositories**: Spring Data JPA repositories interfacing with database entities.
  - **Exception Handling**: `@RestControllerAdvice` converting exceptions to consistent, sanitized JSON responses without exposing raw stack traces.
  - **Security Foundation**: Pre-configured SecurityFilterChain with statutory public permits for health/info endpoints and stateless CORS handling.

### 2.3 Database Layer (`azhost` PostgreSQL)
- **Technology**: PostgreSQL 15 running in a Docker container with named Docker volume (`postgres_data`).
- **Phase 1 Schema**: Foundational `users` table supporting UUID primary keys, email indexing, password hashes, and automated creation/update timestamps.

### 2.4 Infrastructure & Container Layer (`infrastructure/` & `docker-compose.yml`)
- **Docker Compose**: Orchestrates multi-container services (`postgres`, `backend`, `frontend`).
- **Nginx Gateway**: Handles production frontend static bundle hosting and proxies `/api/*` traffic to the backend container.

---

## 3. Communication Patterns

1. **Client → API Communication**: All requests pass through `/api/*` base path.
2. **CORS & Proxying**: In local development, Vite proxies `/api` calls directly to Spring Boot on `http://localhost:8080`. In production Docker deployments, Nginx routes `/api` directly to `http://backend:8080/api/`.
3. **Environment Security**: Sensitive values (database credentials, ports, endpoints) are controlled via environment variables (`.env`).
