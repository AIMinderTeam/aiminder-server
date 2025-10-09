CREATE TABLE IF NOT EXISTS spring_ai_chat_memory (
    conversation_id VARCHAR(255) NOT NULL,
    message_index SERIAL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id, message_index)
);

CREATE INDEX IF NOT EXISTS idx_spring_ai_chat_memory_conversation_id ON spring_ai_chat_memory(conversation_id);
CREATE INDEX IF NOT EXISTS idx_spring_ai_chat_memory_timestamp ON spring_ai_chat_memory(timestamp);
