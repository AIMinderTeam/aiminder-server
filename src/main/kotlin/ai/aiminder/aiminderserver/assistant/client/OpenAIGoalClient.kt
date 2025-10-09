package ai.aiminder.aiminderserver.assistant.client

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponseDto
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequestDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class OpenAIGoalClient(
  @param:Value("classpath:/prompts/goal_prompt.txt")
  private val systemPrompt: Resource,
  private val openAIClient: OpenAIClient,
) : AssistantClient {
  override suspend fun chat(dto: AssistantRequestDto): AssistantResponseDto =
    openAIClient
      .requestStructuredResponse<AssistantResponseDto>(
        dto = dto,
        systemMessage = systemPrompt,
      )
}
