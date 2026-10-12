ALTER TABLE tasks ADD COLUMN owner_id UUID REFERENCES users (id) ON DELETE CASCADE;

-- Tasks created before authentication existed have no owner. In this project they are
-- demo data, so we remove them. With real data you would backfill a system/admin owner instead.
DELETE FROM tasks WHERE owner_id IS NULL;

ALTER TABLE tasks ALTER COLUMN owner_id SET NOT NULL;
CREATE INDEX idx_tasks_owner ON tasks (owner_id);