CREATE TABLE IF NOT EXISTS user_notification_settings
(
  user_id                       uuid PRIMARY KEY NOT NULL
    CONSTRAINT fk_user_notification_settings_user_id REFERENCES users (user_id) ON DELETE CASCADE,
  ai_feedback_enabled           BOOLEAN          NOT NULL DEFAULT TRUE,
  ai_feedback_notification_time TIME             NOT NULL DEFAULT '09:00:00',
  created_at                    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
