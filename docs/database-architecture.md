# AZHost Database Architecture & Entity Relationship Roadmap

This document defines the relational database architecture for AZHost. In Phase 1, only the foundational `users` entity is created in the database. Future entity models documented below will be added in subsequent phases.

---

## 1. Entity Relationship Overview

```text
       +------------------+
       |   github_conn    |
       +------------------+
                | 1:1
                v
       +------------------+
       |      users       |
       +------------------+
                | 1:N
                v
       +------------------+ 1:N +------------------------+
       |     projects     |---->|  environment_variables |
       +------------------+     +------------------------+
            |        |
        1:N |        | 1:N
            v        v
   +--------------+ +------------------+
   | deployments  | |     domains      |
   +--------------+ +------------------+
          | 1:N
          v
   +------------------+
   | deployment_logs  |
   +------------------+
```

---

### 2. Migration Tooling

Flyway (`flyway-core`, `flyway-database-postgresql`) manages schema DDL migrations sequentially:
- `V1__init_schema.sql`: Creates baseline `users` table.
- `V2__create_projects_table.sql`: Creates `projects` table, foreign keys, unique slug constraint, and performance indexes.

---

## Entity Schemas

### Users (`users`)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY | Unique user identifier |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | User email address |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt password hash |
| `display_name` | VARCHAR(100) | NOT NULL | User display name |
| `created_at` | TIMESTAMPTZ | NOT NULL | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Update timestamp |

---

### Projects (`projects`)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY | Unique project identifier |
| `user_id` | UUID | NOT NULL, FK(users.id) | Owning user ID |
| `name` | VARCHAR(100) | NOT NULL | Human-readable project name |
| `slug` | VARCHAR(100) | NOT NULL | URL-safe unique project slug |
| `description` | TEXT | NULLABLE | Detailed description |
| `framework` | VARCHAR(50) | NOT NULL | REACT, VITE, NEXT_JS, VUE, ANGULAR, STATIC, UNKNOWN |
| `source_type` | VARCHAR(50) | NOT NULL | GITHUB, UPLOAD, LOCAL |
| `repository_url` | VARCHAR(255) | NULLABLE | Source GitHub URL |
| `repository_branch` | VARCHAR(100) | NULLABLE | Source Git branch |
| `status` | VARCHAR(50) | NOT NULL | ACTIVE, ARCHIVED |
| `created_at` | TIMESTAMPTZ | NOT NULL | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Update timestamp |

**Constraints & Indexes**:
- `CONSTRAINT uk_projects_user_slug UNIQUE (user_id, slug)`
- `idx_projects_user_slug (user_id, slug)`
- `idx_projects_user_name (user_id, name)`

---

### 2.2 Future Entities Roadmap (Phases 3–9)

#### 2.2.1 `github_connections` (Phase 7)
Stores GitHub OAuth tokens and account mappings per user.
- `id` (UUID, PK)
- `user_id` (UUID, FK -> `users.id`, UNIQUE)
- `github_user_id` (VARCHAR(100))
- `github_username` (VARCHAR(100))
- `access_token_encrypted` (TEXT)
- `created_at`, `updated_at`

#### 2.2.2 `projects` (Phase 3)
Represents a developer application repository imported for deployment.
- `id` (UUID, PK)
- `user_id` (UUID, FK -> `users.id`)
- `name` (VARCHAR(100), NOT NULL)
- `slug` (VARCHAR(100), UNIQUE, NOT NULL)
- `repository_url` (VARCHAR(255))
- `branch` (VARCHAR(100), DEFAULT 'main')
- `framework_type` (VARCHAR(50)) -- e.g., REACT, VITE, NEXTJS, SPRING_BOOT
- `build_command` (VARCHAR(255))
- `output_directory` (VARCHAR(255))
- `created_at`, `updated_at`

#### 2.2.3 `environment_variables` (Phase 3)
Key-value pairs configured for a specific project.
- `id` (UUID, PK)
- `project_id` (UUID, FK -> `projects.id`)
- `key` (VARCHAR(100), NOT NULL)
- `value_encrypted` (TEXT, NOT NULL)
- `environment` (VARCHAR(50)) -- e.g., PRODUCTION, PREVIEW
- `created_at`, `updated_at`

#### 2.2.4 `deployments` (Phase 5 & 6)
Represents an individual build & deployment attempt.
- `id` (UUID, PK)
- `project_id` (UUID, FK -> `projects.id`)
- `commit_hash` (VARCHAR(40))
- `commit_message` (TEXT)
- `status` (VARCHAR(50)) -- e.g., QUEUED, BUILDING, SUCCESS, FAILED
- `trigger_type` (VARCHAR(50)) -- e.g., MANUAL, GITHUB_WEBHOOK
- `duration_ms` (BIGINT)
- `created_at`, `updated_at`

#### 2.2.5 `deployment_logs` (Phase 5)
Real-time build and stdout/stderr execution logs attached to a deployment.
- `id` (UUID, PK)
- `deployment_id` (UUID, FK -> `deployments.id`)
- `log_level` (VARCHAR(20)) -- e.g., INFO, WARN, ERROR
- `message` (TEXT, NOT NULL)
- `timestamp` (TIMESTAMP WITH TIME ZONE)

#### 2.2.6 `domains` (Phase 9)
Custom domains and auto-assigned subdomains pointing to a project.
- `id` (UUID, PK)
- `project_id` (UUID, FK -> `projects.id`)
- `domain_name` (VARCHAR(255), UNIQUE, NOT NULL)
- `is_primary` (BOOLEAN, DEFAULT FALSE)
- `ssl_status` (VARCHAR(50)) -- e.g., PENDING, ACTIVE, FAILED
- `created_at`, `updated_at`
