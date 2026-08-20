-- Flyway V14: Add Request ID Column to Audit Logs for Request Correlation

ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS request_id VARCHAR(255);
