-- Phase 11: Distributed Scalability, High Availability & Worker Architecture Migration

-- Add worker claim and heartbeat tracking to project_builds
ALTER TABLE project_builds ADD COLUMN claimed_by VARCHAR(255);
ALTER TABLE project_builds ADD COLUMN claimed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE project_builds ADD COLUMN heartbeat_at TIMESTAMP WITH TIME ZONE;

-- Create indexes for efficient distributed queue polling and heartbeat tracking
CREATE INDEX idx_project_builds_queued ON project_builds (status, created_at);
CREATE INDEX idx_project_builds_claimed ON project_builds (claimed_by, status);

-- Add storage quota reservation column to projects
ALTER TABLE projects ADD COLUMN reserved_storage_bytes BIGINT NOT NULL DEFAULT 0;

-- Create DB table for distributed OAuth CSRF state persistence
CREATE TABLE oauth_state_tokens (
    state_token VARCHAR(255) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_oauth_state_expires ON oauth_state_tokens (expires_at);
