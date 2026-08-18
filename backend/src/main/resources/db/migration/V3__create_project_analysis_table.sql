-- Flyway V3: Create Project Analysis Table

CREATE TABLE IF NOT EXISTS project_analysis (
    project_id UUID PRIMARY KEY REFERENCES projects(id) ON DELETE CASCADE,
    framework VARCHAR(50) NOT NULL,
    framework_confidence VARCHAR(20) NOT NULL,
    build_tool VARCHAR(50),
    package_manager VARCHAR(50) NOT NULL,
    package_manager_confidence VARCHAR(20) NOT NULL,
    language VARCHAR(50) NOT NULL,
    build_command VARCHAR(255),
    dev_command VARCHAR(255),
    output_directory VARCHAR(100) NOT NULL,
    node_version VARCHAR(50),
    confidence VARCHAR(20) NOT NULL,
    executed BOOLEAN NOT NULL DEFAULT FALSE,
    evidence TEXT,
    warnings TEXT,
    detected_files TEXT,
    analyzed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_project_analysis_analyzed_at ON project_analysis(analyzed_at);
