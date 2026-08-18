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

## 2. Entity Schemas & Models

### 2.1 `users` Table (Phase 1 Baseline)
Stores platform developer accounts.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique user identifier |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | Account email address |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt / Argon2 password hash |
| `display_name` | VARCHAR(100) | NOT NULL | User's full name or handle |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Account creation timestamp |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last profile update timestamp |

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
