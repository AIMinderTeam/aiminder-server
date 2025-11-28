package ai.aiminder.aiminderserver.assistant.scheduler

import ai.aiminder.aiminderserver.assistant.service.FeedbackService
import ai.aiminder.aiminderserver.common.util.logger
import ai.aiminder.aiminderserver.conversation.service.ConversationService
import ai.aiminder.aiminderserver.goal.service.GoalService
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.service.UserService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FeedbackScheduler(
  private val userService: UserService,
  private val goalService: GoalService,
  private val conversationService: ConversationService,
  private val feedbackService: FeedbackService,
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
            .getByUserId(user.id)
            .collect { goal ->
              runCatching {
                val conversation = conversationService.getByGoal(goal)
                feedbackService.feedback(goal, user, conversation)
                logger.info("Feedback processed for goal ${goal.id}, conversation ${conversation.id}")
              }.getOrElse {
                logger.error("Error processing feedback for goal ${goal.id}: ${it.message}", it)
              }
            }
        }
      logger.info("Finished feedback scheduler")
    }.getOrElse {
      logger.error("Error in feedback scheduler: ${it.message}", it)
    }
}
