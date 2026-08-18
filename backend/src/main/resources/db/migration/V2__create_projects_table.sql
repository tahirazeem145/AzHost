-- Flyway V2: Create Projects Relational Table

CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description TEXT,
    framework VARCHAR(50) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    repository_url VARCHAR(255),
    repository_branch VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_projects_user_slug UNIQUE (user_id, slug)
);

CREATE INDEX IF NOT EXISTS idx_projects_user_slug ON projects(user_id, slug);
CREATE INDEX IF NOT EXISTS idx_projects_user_name ON projects(user_id, name);
