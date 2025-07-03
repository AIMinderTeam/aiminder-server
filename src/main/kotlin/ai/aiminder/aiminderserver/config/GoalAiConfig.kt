package ai.aiminder.aiminderserver.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

@Configuration
class GoalAiConfig(
    @Value("classpath:/prompts/goal_prompt.txt")
    private val prompt: Resource,
) {
    @Bean
    fun goalChatClient(builder: ChatClient.Builder): ChatClient =
        builder
            .defaultSystem(prompt)
            .build()
}
