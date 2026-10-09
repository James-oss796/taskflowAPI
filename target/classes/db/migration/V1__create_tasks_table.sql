CREATE TABLE tasks (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title   VARCHAR(200) NOT NULL,
    description TEXT,
    status  VARCHAR(20) NOT NULL DEFAULT 'TODO',
    due_date    DATE,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);