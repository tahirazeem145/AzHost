-- Flyway V10: Add auto-deploy settings to projects and create webhook delivery tracking table

-- Auto-deploy settings on projects
ALTER TABLE projects ADD COLUMN IF NOT EXISTS auto_deploy BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS auto_deploy_branch VARCHAR(255);
ALTER TABLE projects ADD COLUMN IF NOT EXISTS encrypted_webhook_secret TEXT;

-- Webhook delivery deduplication tracking table
CREATE TABLE IF NOT EXISTS github_webhook_deliveries (
    id                UUID PRIMARY KEY,
    project_id        UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    delivery_id       VARCHAR(255) NOT NULL,
    event_type        VARCHAR(100),
    commit_sha        VARCHAR(64),
    branch            VARCHAR(255),
    status            VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
    received_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at      TIMESTAMP WITH TIME ZONE,
    error_message     TEXT,
    CONSTRAINT uk_webhook_delivery UNIQUE (project_id, delivery_id)
);

CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_project_id ON github_webhook_deliveries(project_id);
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_delivery_id ON github_webhook_deliveries(delivery_id);
