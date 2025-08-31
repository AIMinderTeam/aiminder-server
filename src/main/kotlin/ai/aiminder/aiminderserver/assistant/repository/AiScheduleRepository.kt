package ai.aiminder.aiminderserver.assistant.repository

import ai.aiminder.aiminderserver.assistant.domain.AiSchedule
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class AiScheduleRepository {
  private val database = mutableSetOf<AiSchedule>()
  private val logger = LoggerFactory.getLogger(this::class.java)

  fun save(aiSchedules: List<AiSchedule>): List<AiSchedule> {
    logger.info("Saving schedules: $aiSchedules")
    database.addAll(aiSchedules)
    return aiSchedules
  }
}