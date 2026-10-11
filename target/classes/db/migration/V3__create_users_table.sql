CREATE TABLE users (
    id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email   VARCHAR(254) NOT NULL ,
    password_hash   VARCHAR(255) NOT NULL ,
    role    VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT  now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_email_lowercase CHECK ( email = lower(email) )
);