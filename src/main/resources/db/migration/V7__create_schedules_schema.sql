CREATE TABLE schedules
(
  schedule_id UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
  goal_id     UUID         NOT NULL,
  user_id     UUID         NOT NULL,
  title       VARCHAR(255) NOT NULL,
  description TEXT,
  status      VARCHAR(20)  NOT NULL DEFAULT 'READY',
  start_date  TIMESTAMP    NOT NULL,
  end_date    TIMESTAMP    NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at  TIMESTAMP,

  CONSTRAINT fk_schedules_goal FOREIGN KEY (goal_id)
    REFERENCES goals (goal_id) ON DELETE CASCADE,
  CONSTRAINT fk_schedules_user FOREIGN KEY (user_id)
    REFERENCES users (user_id) ON DELETE CASCADE,
  CONSTRAINT chk_schedule_dates CHECK (start_date <= end_date)
);

CREATE INDEX idx_schedules_user_id ON schedules (user_id);
CREATE INDEX idx_schedules_goal_id ON schedules (goal_id);
CREATE INDEX idx_schedules_status ON schedules (status);
CREATE INDEX idx_schedules_dates ON schedules (start_date, end_date);
CREATE INDEX idx_schedules_deleted_at ON schedules (deleted_at) WHERE deleted_at IS NULL;
