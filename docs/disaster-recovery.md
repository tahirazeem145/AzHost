# AZHost Disaster Recovery Guide

This document details back up, encryption key recovery, and system restore procedures.

## Recovery Point Objective (RPO) & Recovery Time Objective (RTO)
* **RPO**: 1 hour (maximum allowed data loss of database transactions).
* **RTO**: 15 minutes (maximum service down-time using standard Docker Compose restore).

## Backup Strategies
### 1. Database Backup (PostgreSQL)
Run a cron job executing `pg_dump` hourly:
```bash
pg_dump -U azhost_prod -h db.example.com azhost > /backups/db/azhost_$(date +%F_%H).sql
```
Encrypt backups with GPG and store them in an offsite bucket (e.g. AWS S3 Glacier).

### 2. Artifact and Workspace Directories
Backup `/app/artifacts` hourly. Do not backup `/app/workspaces` as workspaces are transient build environments and can be re-cloned from GitHub on demand.

## Encryption Key Recovery
> [!IMPORTANT]
> The `ENCRYPTION_KEY` is used to encrypt and decrypt GitHub oauth access tokens.
> If the key is lost, existing repository integration tokens cannot be decrypted.
> Users must disconnect and re-authenticate their repositories.
> Store the `ENCRYPTION_KEY` securely in a hardware security module (HSM) or password manager (Vault).

## Restore Procedure
1. Boot a fresh server instance with Docker installed.
2. Setup the `.env` file containing original environment keys including the original `ENCRYPTION_KEY`.
3. Restore database schema and records:
   ```bash
   psql -U azhost_prod -h db.example.com azhost < /backups/db/azhost_latest.sql
   ```
4. Restore artifact zip files into `/app/artifacts` volume.
5. Launch the compose stack:
   ```bash
   docker compose -f docker-compose.production.yml up -d
   ```
