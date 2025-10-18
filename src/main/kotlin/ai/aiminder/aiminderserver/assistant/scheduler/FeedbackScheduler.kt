package ai.aiminder.aiminderserver.assistant.scheduler

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.dto.ChatResponse
import ai.aiminder.aiminderserver.assistant.service.AssistantService
import ai.aiminder.aiminderserver.assistant.service.ChatService
import ai.aiminder.aiminderserver.common.util.logger
import ai.aiminder.aiminderserver.conversation.domain.Conversation
import ai.aiminder.aiminderserver.conversation.service.ConversationService
import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.service.GoalService
import ai.aiminder.aiminderserver.schedule.domain.Schedule
import ai.aiminder.aiminderserver.schedule.service.ScheduleService
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.service.UserService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

@Component
class FeedbackScheduler(
  private val assistantService: AssistantService,
  private val conversationService: ConversationService,
  private val userService: UserService,
  private val goalService: GoalService,
  private val scheduleService: ScheduleService,
  private val chatService: ChatService,
) {
  private val logger = logger()

  @Scheduled(cron = "0 0 9 * * *")
  suspend fun feedback() =
    runCatching {
      logger.info("Starting feedback scheduler")
      userService
        .getUsers()
        .collect { user: User ->
          goalService
            .get(user.id)
            .collect { goal ->
              val yesterdaySchedules: List<Schedule> = getYesterdaySchedules(goal)
              val todaySchedules: List<Schedule> = getTodaySchedules(goal)
              val conversation: Conversation = conversationService.getByGoal(goal)
              val assistantResponse: AssistantResponse =
                assistantService.feedback(
                  user = user,
                  conservation = conversation,
                  goal = goal,
                  yesterdaySchedules = yesterdaySchedules,
                  todaySchedules = todaySchedules,
                )
              val chatResponse = ChatResponse.from(conversation, assistantResponse)
              chatService.create(chatResponse)
            }
        }
      logger.info("Finished feedback scheduler")
    }.getOrElse {
      logger.error("Error in feedback scheduler: ${it.message}", it)
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

  private fun getEndDate(yesterday: LocalDateTime): Instant =
    yesterday
      .withHour(23)
      .withMinute(59)
      .withSecond(59)
      .toInstant(UTC)

  private fun getStartDate(yesterday: LocalDateTime): Instant =
    yesterday
      .withHour(0)
      .withMinute(0)
      .withSecond(0)
      .toInstant(UTC)
}
