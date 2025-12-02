CREATE TABLE IF NOT EXISTS user_notification_settings
(
  notification_setting_id       uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
  user_id                       uuid             NOT NULL
    CONSTRAINT fk_user_notification_settings_user_id REFERENCES users (user_id) ON DELETE CASCADE,
  ai_feedback_enabled           BOOLEAN          NOT NULL DEFAULT TRUE,
  ai_feedback_notification_time TIME             NOT NULL DEFAULT '09:00:00',
  created_at                    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- user_id에 대한 인덱스 생성 (이미 존재하면 무시)
CREATE INDEX IF NOT EXISTS idx_user_notification_settings_user_id ON user_notification_settings (user_id);

-- 기존의 모든 사용자에 대해 알림 설정이 없는 경우 기본 설정 추가
INSERT INTO user_notification_settings (user_id, ai_feedback_enabled, ai_feedback_notification_time)
SELECT u.user_id, TRUE, '09:00:00'::TIME
FROM users u
WHERE NOT EXISTS (SELECT 1
                  FROM user_notification_settings uns
                  WHERE uns.user_id = u.user_id);
