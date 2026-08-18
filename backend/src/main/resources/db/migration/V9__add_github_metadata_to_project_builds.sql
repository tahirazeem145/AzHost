-- Flyway V9: Add GitHub Source metadata columns to project_builds table

ALTER TABLE project_builds ADD COLUMN IF NOT EXISTS source_type VARCHAR(50);
ALTER TABLE project_builds ADD COLUMN IF NOT EXISTS github_repository_id BIGINT;
ALTER TABLE project_builds ADD COLUMN IF NOT EXISTS github_commit_sha VARCHAR(64);
