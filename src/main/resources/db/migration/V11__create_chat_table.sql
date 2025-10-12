CREATE TABLE IF NOT EXISTS chat
(
  chat_id         BIGSERIAL PRIMARY KEY NOT NULL,
  conversation_id UUID                  NOT NULL,
  content         TEXT                  NOT NULL,
  type            VARCHAR(50)           NOT NULL,
  created_at      TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_chat_conversation FOREIGN KEY (conversation_id)
    REFERENCES conversations (conversation_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chat_conversation_id ON chat (conversation_id);
