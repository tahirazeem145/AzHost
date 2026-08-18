-- Flyway V6: Add Active Deployment Column to Projects Table

ALTER TABLE projects ADD COLUMN IF NOT EXISTS active_deployment_id UUID REFERENCES deployments(id) ON DELETE SET NULL;
