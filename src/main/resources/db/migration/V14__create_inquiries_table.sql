-- 문의/후기 테이블 생성
CREATE TABLE IF NOT EXISTS inquiries
(
  inquiry_id    uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
  user_id       uuid             NOT NULL
    CONSTRAINT fk_inquiries_user_id REFERENCES users (user_id) ON DELETE CASCADE,
  inquiry_type  VARCHAR(50)      NOT NULL 
    CHECK (inquiry_type IN ('REVIEW', 'BUG_REPORT', 'IMPROVEMENT_SUGGESTION', 'GENERAL')),
  content       TEXT             NOT NULL CHECK (char_length(content) <= 1000),
  contact_email VARCHAR(255),
  status        VARCHAR(20)      NOT NULL DEFAULT 'PENDING'
    CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED')),
  created_at    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at    TIMESTAMP
);
