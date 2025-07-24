package ai.aiminder.aiminderserver.client

import ai.aiminder.aiminderserver.domain.AssistantResponse
import ai.aiminder.aiminderserver.dto.AssistantRequest
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class OpenAIGoalClient(
    @Value("classpath:/prompts/goal_prompt.txt")
    private val systemPrompt: Resource,
    private val openAIClient: OpenAIClient,
) : AssistantClient {
    override suspend fun chat(assistantRequest: AssistantRequest): AssistantResponse =
        openAIClient
            .requestStructuredResponse<AssistantResponse>(
                systemMessage = systemPrompt,
                userMessage = assistantRequest.text,
            )

    override suspend fun chat(
        conversationId: String,
        userMessage: String,
    ): Flow<String> = openAIClient.request(systemPrompt, userMessage, conversationId)
}
