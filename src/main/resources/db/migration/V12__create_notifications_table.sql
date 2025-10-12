CREATE TABLE IF NOT EXISTS notifications
(
  notification_id uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
  type            VARCHAR(30)      NOT NULL,
  title           VARCHAR(255)     NOT NULL,
  description     TEXT             NOT NULL,
  metadata        JSONB            NOT NULL,
  checked         BOOLEAN          NOT NULL,
  receiver_id     uuid             NOT NULL
    CONSTRAINT fk_notifications_receiver_id REFERENCES users (user_id) ON DELETE CASCADE,
  created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at      TIMESTAMP
);

CREATE INDEX idx_notifications_receiver_created
  ON notifications (receiver_id, created_at DESC);

CREATE INDEX idx_notifications_receiver_checked
  ON notifications (receiver_id, checked)
  WHERE checked = FALSE;
