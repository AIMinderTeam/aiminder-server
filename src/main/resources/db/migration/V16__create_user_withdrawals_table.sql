-- 회원 탈퇴 사유 기록 테이블
CREATE TABLE IF NOT EXISTS user_withdrawals
(
  withdrawal_id uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
  user_id       uuid             NOT NULL
    CONSTRAINT fk_user_withdrawals_user_id REFERENCES users (user_id) ON DELETE CASCADE,
  reason        VARCHAR(50)      NOT NULL 
    CHECK (reason IN ('SERVICE_DISSATISFACTION', 'USING_OTHER_SERVICE', 'PRIVACY_CONCERN', 'LOW_USAGE_FREQUENCY', 'OTHER')),
  created_at    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_withdrawals_user_id ON user_withdrawals (user_id);
