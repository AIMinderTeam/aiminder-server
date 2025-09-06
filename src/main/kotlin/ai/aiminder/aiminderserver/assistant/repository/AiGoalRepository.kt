package ai.aiminder.aiminderserver.assistant.repository

import ai.aiminder.aiminderserver.assistant.domain.AiGoal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class AiGoalRepository {
  private val database = mutableSetOf<AiGoal>()
  private val logger = LoggerFactory.getLogger(this::class.java)

  fun save(aiGoal: AiGoal): AiGoal {
    logger.info("Saving goal: $aiGoal")
    database.add(aiGoal)
    return aiGoal
  }
}
