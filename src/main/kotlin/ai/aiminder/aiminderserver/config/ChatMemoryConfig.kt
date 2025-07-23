package ai.aiminder.aiminderserver.config

import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ChatMemoryConfig {
    @Bean
    fun chatMemory() = MessageWindowChatMemory.builder().maxMessages(20).build()
}
