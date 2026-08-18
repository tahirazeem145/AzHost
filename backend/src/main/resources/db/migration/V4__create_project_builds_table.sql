-- Flyway V4: Create Project Builds Relational Table

CREATE TABLE IF NOT EXISTS project_builds (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    framework VARCHAR(50) NOT NULL,
    package_manager VARCHAR(50) NOT NULL,
    node_version VARCHAR(50) NOT NULL,
    build_command VARCHAR(255),
    output_directory VARCHAR(100) NOT NULL,
    workspace_id VARCHAR(100) NOT NULL,
    artifact_id VARCHAR(100),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    exit_code INT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_project_builds_project_id ON project_builds(project_id);
CREATE INDEX IF NOT EXISTS idx_project_builds_status ON project_builds(status);
CREATE INDEX IF NOT EXISTS idx_project_builds_created_at ON project_builds(created_at);
