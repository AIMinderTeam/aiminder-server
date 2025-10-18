package ai.aiminder.aiminderserver.assistant.service

import ai.aiminder.aiminderserver.assistant.client.FeedbackAssistantClient
import ai.aiminder.aiminderserver.assistant.client.GoalAssistantClient
import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequestDto
import ai.aiminder.aiminderserver.conversation.domain.Conversation
import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.schedule.domain.Schedule
import ai.aiminder.aiminderserver.user.domain.User
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service

@Service
class AssistantService(
  @param:Value("classpath:/prompts/welcome_message.txt")
  private val welcomeMessageResource: Resource,
  private val chatMemory: ChatMemory,
  private val goalAssistantClient: GoalAssistantClient,
  private val feedbackAssistantClient: FeedbackAssistantClient,
  objectMapper: ObjectMapper,
) {
  private val assistantResponse: AssistantResponse = AssistantResponse.from(welcomeMessageResource)
  private val welcomeMessage: String = objectMapper.writeValueAsString(assistantResponse)
  private val assistantMessage = AssistantMessage(welcomeMessage)

  suspend fun sendMessage(dto: AssistantRequestDto): AssistantResponse = goalAssistantClient.chat(dto)

  suspend fun startChat(conversation: Conversation): AssistantResponse {
    chatMemory.add(conversation.id.toString(), assistantMessage)
    return assistantResponse
  }

  suspend fun feedback(
    user: User,
    conservation: Conversation,
    goal: Goal,
    yesterdaySchedules: List<Schedule>,
    todaySchedules: List<Schedule>,
  ): AssistantResponse {
    val dto =
      AssistantRequestDto(
        conversationId = conservation.id,
        userId = user.id,
        text =
          """
          목표: $goal
          어제 일정 목록: $yesterdaySchedules
          오늘 일정 목록: $todaySchedules
          """.trimIndent(),
        goalId = goal.id,
      )
    return feedbackAssistantClient.chat(dto)
  }
}
