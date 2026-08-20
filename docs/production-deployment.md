# AZHost Production Deployment Guide

This guide details the steps required to deploy AZHost in a production environment.

## Prerequisites
* Java 17+ (for bare-metal execution) or Docker Engine 20.10+
* Docker Compose v2+
* PostgreSQL database instance (version 14+)
* GitHub OAuth application credentials
* Supabase account

## Environment Variables
The following environment variables must be configured on the host system:

| Variable | Description | Example / Note |
| --- | --- | --- |
| `DATABASE_URL` | JDBC URL for production PostgreSQL database | `jdbc:postgresql://db.example.com:5432/azhost` |
| `DATABASE_USERNAME` | Production database user name | `azhost_prod` |
| `DATABASE_PASSWORD` | Production database password | (Strong random password) |
| `SUPABASE_URL` | Base URL of the Supabase API | `https://xyz.supabase.co` |
| `SUPABASE_SERVICE_ROLE_KEY` | Supabase service role API key | (Sensitive API key) |
| `GITHUB_CLIENT_ID` | GitHub OAuth client ID | `Iv1.1234567890abcdef` |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth client secret | (Sensitive client secret) |
| `GITHUB_WEBHOOK_SECRET` | Secret signature used by GitHub webhooks | (Random string) |
| `ENCRYPTION_KEY` | 32-character AES key for encrypting repo tokens | (Keep highly secure!) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed frontend origins | `https://app.azhost.com` |
| `AZHOST_PUBLIC_URL` | Public callback URL of this backend service | `https://api.azhost.com` |

## Deployment Steps
1. Clone the repository and configure the production environment variables in a secure `.env` file.
2. Build the production Docker image:
   ```bash
   docker compose -f docker-compose.production.yml build
   ```
3. Boot the stack:
   ```bash
   docker compose -f docker-compose.production.yml up -d
   ```
4. Verify deployment health:
   ```bash
   curl -f http://localhost:8080/api/health
   curl -f http://localhost:8080/api/ready
   ```

## Log Collection & Observability
* Standard output logs are formatted with correlation IDs as `[reqId=XXXX]`.
* Monitor Prometheus metrics via `/actuator/prometheus`.
