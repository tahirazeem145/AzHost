-- Flyway V12: Add deterministic deployment sequence numbers per project

-- 1. Add columns
ALTER TABLE deployments ADD COLUMN sequence_number BIGINT;
ALTER TABLE projects ADD COLUMN deployment_counter BIGINT DEFAULT 0 NOT NULL;

-- 2. Backfill existing deployments sequence numbers deterministically per project
UPDATE deployments d
SET sequence_number = (
    SELECT seq
    FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY project_id ORDER BY created_at, id) as seq
        FROM deployments
    ) sub
    WHERE sub.id = d.id
);

-- 3. If any sequence_number is still null (e.g. no rows to update or corner cases), default it to 0
UPDATE deployments SET sequence_number = 0 WHERE sequence_number IS NULL;

-- 4. Set NOT NULL constraint
ALTER TABLE deployments ALTER COLUMN sequence_number SET NOT NULL;

-- 5. Update project deployment counters to match the max sequence number assigned
UPDATE projects p
SET deployment_counter = COALESCE(
    (SELECT MAX(sequence_number) FROM deployments d WHERE d.project_id = p.id),
    0
);
