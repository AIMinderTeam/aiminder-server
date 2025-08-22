package ai.aiminder.aiminderserver.assistant.client

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OpenAIGoalClient(
  @Value("classpath:/prompts/goal_prompt.txt")
  private val systemPrompt: Resource,
  private val openAIClient: OpenAIClient,
) : AssistantClient {
  override suspend fun chat(
    conversationId: UUID,
    assistantRequest: AssistantRequest,
  ): AssistantResponse =
    openAIClient
      .requestStructuredResponse<AssistantResponse>(
        systemMessage = systemPrompt,
        userMessage = assistantRequest.text,
        conversationId = conversationId,
      )
}