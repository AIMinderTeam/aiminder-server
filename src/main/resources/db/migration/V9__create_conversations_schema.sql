CREATE TABLE conversations
(
  conversation_id UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
  user_id         UUID         NOT NULL,
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at      TIMESTAMP,

  CONSTRAINT fk_conversations_user FOREIGN KEY (user_id)
    REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE INDEX idx_conversations_user_id ON conversations (user_id);
CREATE INDEX idx_conversations_deleted_at ON conversations (deleted_at) WHERE deleted_at IS NULL;