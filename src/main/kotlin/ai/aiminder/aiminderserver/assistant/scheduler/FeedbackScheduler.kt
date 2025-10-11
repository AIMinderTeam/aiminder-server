package ai.aiminder.aiminderserver.assistant.scheduler

import ai.aiminder.aiminderserver.assistant.service.AssistantService
import ai.aiminder.aiminderserver.common.util.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FeedbackScheduler(
  private val assistantService: AssistantService,
) {
  private val logger = logger()

  @Scheduled(cron = "0 0 12 * * *")
  suspend fun feedback() {
  }
}
