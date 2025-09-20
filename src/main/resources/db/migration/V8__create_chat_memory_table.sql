-- Spring AI Chat Memory 테이블 생성
-- 채팅 메모리를 PostgreSQL에 영구 저장하기 위한 테이블
-- 스키마는 Spring AI JdbcChatMemoryRepository가 실제로 사용하는 형식에 맞춤
CREATE TABLE IF NOT EXISTS spring_ai_chat_memory (
    conversation_id VARCHAR(255) NOT NULL,
    message_index SERIAL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id, message_index)
);

-- 성능 최적화를 위한 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_spring_ai_chat_memory_conversation_id ON spring_ai_chat_memory(conversation_id);
CREATE INDEX IF NOT EXISTS idx_spring_ai_chat_memory_timestamp ON spring_ai_chat_memory(timestamp);