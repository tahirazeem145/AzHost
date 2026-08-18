-- Flyway V7: Create GitHub Connections Relational Table

CREATE TABLE IF NOT EXISTS github_connections (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    github_user_id BIGINT NOT NULL UNIQUE,
    github_username VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(1000),
    encrypted_access_token TEXT NOT NULL,
    scopes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_github_connections_user_id ON github_connections(user_id);
