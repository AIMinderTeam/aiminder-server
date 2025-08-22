CREATE TABLE refresh_token
(
    refresh_token_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id          UUID        NOT NULL,
    token            TEXT        NOT NULL,
    registered_date  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    modified_date    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uq_refresh_token_token UNIQUE (token)
);

CREATE INDEX idx_refresh_token_user_id ON refresh_token (user_id);
