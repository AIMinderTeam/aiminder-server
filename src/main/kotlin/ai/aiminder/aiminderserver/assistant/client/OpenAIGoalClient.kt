package ai.aiminder.aiminderserver.assistant.client

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequestDto
import ai.aiminder.aiminderserver.assistant.tool.GoalTool
import ai.aiminder.aiminderserver.assistant.tool.TodayTool
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class OpenAIGoalClient(
  @param:Value("classpath:/prompts/goal_prompt.txt")
  private val systemPrompt: Resource,
  private val goalTool: GoalTool,
  private val todayTool: TodayTool,
) : OpenAIClient(),
  AssistantClient {
  override suspend fun chat(dto: AssistantRequestDto): AssistantResponse =
    requestStructuredResponse<AssistantResponse>(
      dto = dto,
      systemMessage = systemPrompt,
    )

  override fun setTools(requestSpec: ChatClient.ChatClientRequestSpec): ChatClient.ChatClientRequestSpec =
    requestSpec.tools(goalTool, todayTool)
}
