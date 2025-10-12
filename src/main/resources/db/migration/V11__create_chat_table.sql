CREATE TABLE IF NOT EXISTS chat
(
  chat_id         UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
  conversation_id UUID             NOT NULL,
  message_index   BIGSERIAL,
  content         TEXT             NOT NULL,
  type            VARCHAR(50)      NOT NULL,
  created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_chat_conversation FOREIGN KEY (conversation_id)
    REFERENCES conversations (conversation_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chat_conversation_id ON chat (conversation_id);
