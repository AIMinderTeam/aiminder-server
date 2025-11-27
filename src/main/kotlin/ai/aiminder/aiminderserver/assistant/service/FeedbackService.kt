package ai.aiminder.aiminderserver.assistant.service

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.dto.ChatResponse
import ai.aiminder.aiminderserver.conversation.domain.Conversation
import ai.aiminder.aiminderserver.conversation.service.ConversationService
import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.service.GoalService
import ai.aiminder.aiminderserver.schedule.domain.Schedule
import ai.aiminder.aiminderserver.schedule.service.ScheduleService
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID

@Service
class FeedbackService(
  private val assistantService: AssistantService,
  private val conversationService: ConversationService,
  private val chatService: ChatService,
  private val scheduleService: ScheduleService,
  private val goalService: GoalService,
) {
  suspend fun feedback(
    conversationId: UUID,
    user: User,
  ): ChatResponse {
    val goal: Goal = goalService.getByConversationId(conversationId, user.id)
    return feedback(goal, user)
      ?: ChatResponse.from(conversationId, AssistantResponse.from("피드백 할 일정이 존재하지 않습니다."))
  }

  suspend fun feedback(
    goal: Goal,
    user: User,
  ): ChatResponse? {
    val yesterdaySchedules: List<Schedule> = getYesterdaySchedules(goal)
    val todaySchedules: List<Schedule> = getTodaySchedules(goal)
    val conversation: Conversation = conversationService.getByGoal(goal)
    if (yesterdaySchedules.isEmpty() && todaySchedules.isEmpty()) {
      return ChatResponse.from(conversation.id, AssistantResponse.from("피드백 할 일정이 존재하지 않습니다."))
    }
    val assistantResponse: AssistantResponse =
      assistantService.feedback(
        user = user,
        conservation = conversation,
        goal = goal,
        yesterdaySchedules = yesterdaySchedules,
        todaySchedules = todaySchedules,
      )
    val chatResponse = ChatResponse.from(conversation.id, assistantResponse)
    chatService.create(chatResponse)
    return chatResponse
  }

  private suspend fun getYesterdaySchedules(goal: Goal): List<Schedule> {
    val yesterday = LocalDateTime.now().minusDays(1)
    val startDate = getStartDate(yesterday)
    val endDate = getEndDate(yesterday)
    return scheduleService.get(goal.id, startDate, endDate)
  }

  private suspend fun getTodaySchedules(goal: Goal): List<Schedule> {
    val today = LocalDateTime.now()
    val startDate = getStartDate(today)
    val endDate = getEndDate(today)
    return scheduleService.get(goal.id, startDate, endDate)
  }

  private fun getEndDate(dateTime: LocalDateTime): Instant =
    dateTime
      .withHour(23)
      .withMinute(59)
      .withSecond(59)
      .toInstant(UTC)

  private fun getStartDate(dateTime: LocalDateTime): Instant =
    dateTime
      .withHour(0)
      .withMinute(0)
      .withSecond(0)
      .toInstant(UTC)
}
