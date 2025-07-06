package ai.aiminder.aiminderserver.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GoalAiConfig {
    @Bean
    fun goalChatClient(builder: ChatClient.Builder): ChatClient = builder.build()
}
