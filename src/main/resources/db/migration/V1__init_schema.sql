CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users
(
    user_id           uuid PRIMARY KEY default uuid_generate_v4(),
    provider          VARCHAR(50)  NOT NULL,
    provider_id       VARCHAR(255) NOT NULL,
    created_at        TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (provider, provider_id)
);

CREATE INDEX idx_users_provider_id ON users (provider, provider_id);