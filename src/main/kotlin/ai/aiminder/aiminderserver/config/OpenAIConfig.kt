package ai.aiminder.aiminderserver.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenAIConfig {
    @Bean
    fun chatClient(
        builder: ChatClient.Builder,
        memory: ChatMemory,
    ): ChatClient =
        builder
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
            .build()
}
