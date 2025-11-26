package ai.aiminder.aiminderserver.assistant.scheduler

import ai.aiminder.aiminderserver.assistant.service.FeedbackService
import ai.aiminder.aiminderserver.common.util.logger
import ai.aiminder.aiminderserver.goal.service.GoalService
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.service.UserService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FeedbackScheduler(
  private val userService: UserService,
  private val goalService: GoalService,
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
              feedbackService.feedback(goal, user)
            }
        }
      logger.info("Finished feedback scheduler")
    }.getOrElse {
      logger.error("Error in feedback scheduler: ${it.message}", it)
    }
}
