CREATE TYPE goal_status AS ENUM ('ACTIVE', 'COMPLETED', 'ARCHIVED');

CREATE TABLE goals
(
  goal_id         UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
  user_id         UUID         NOT NULL,
  title           VARCHAR(500) NOT NULL,
  description     TEXT,
  target_date     TIMESTAMP,
  is_ai_generated BOOLEAN      NOT NULL DEFAULT false,
  status          goal_status  NOT NULL DEFAULT 'ACTIVE',
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at      TIMESTAMP,

  CONSTRAINT fk_goals_user FOREIGN KEY (user_id)
    REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE INDEX idx_goals_user_id ON goals (user_id);
CREATE INDEX idx_goals_status ON goals (status);
CREATE INDEX idx_goals_deleted_at ON goals (deleted_at) WHERE deleted_at IS NULL;
