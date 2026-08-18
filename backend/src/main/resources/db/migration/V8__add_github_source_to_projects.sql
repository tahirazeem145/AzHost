-- Flyway V8: Add GitHub Source metadata columns to projects table

ALTER TABLE projects ADD COLUMN IF NOT EXISTS github_repository_id BIGINT;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS github_repository_name VARCHAR(255);
ALTER TABLE projects ADD COLUMN IF NOT EXISTS github_branch VARCHAR(255);
ALTER TABLE projects ADD COLUMN IF NOT EXISTS github_commit_sha VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_projects_github_repository_id ON projects(github_repository_id);
