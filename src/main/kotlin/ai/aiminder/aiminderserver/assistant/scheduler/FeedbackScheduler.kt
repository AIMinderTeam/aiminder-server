package ai.aiminder.aiminderserver.assistant.scheduler

import ai.aiminder.aiminderserver.assistant.service.FeedbackService
import ai.aiminder.aiminderserver.common.util.logger
import ai.aiminder.aiminderserver.conversation.service.ConversationService
import ai.aiminder.aiminderserver.goal.service.GoalService
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.service.UserNotificationSettingsService
import ai.aiminder.aiminderserver.user.service.UserService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalTime

@Component
class FeedbackScheduler(
  private val userService: UserService,
  private val goalService: GoalService,
  private val conversationService: ConversationService,
  private val feedbackService: FeedbackService,
  private val userNotificationSettingsService: UserNotificationSettingsService,
) {
  private val logger = logger()

  @Scheduled(cron = "0 * * * * *")
  suspend fun feedback() =
    runCatching {
      val currentTime = LocalTime.now().withSecond(0).withNano(0)
      logger.info("Starting feedback scheduler at $currentTime")

      userNotificationSettingsService
        .getUserIdsForFeedbackAtTime(currentTime)
        .collect { userId ->
          runCatching {
            val user = userService.getUserById(userId).let { User.from(it) }
            goalService
              .getByUserId(userId)
              .collect { goal ->
                runCatching {
                  val conversation = conversationService.getByGoal(goal)
                  feedbackService.feedback(goal, user, conversation)
                  logger.info("Feedback processed for goal ${goal.id}, conversation ${conversation.id}")
                }.getOrElse {
                  logger.error("Error processing feedback for goal ${goal.id}: ${it.message}", it)
                }
              }
          }.getOrElse {
            logger.error("Error processing feedback for user $userId: ${it.message}", it)
          }
        }
      logger.info("Finished feedback scheduler at $currentTime")
    }.getOrElse {
      logger.error("Error in feedback scheduler: ${it.message}", it)
    }
}
