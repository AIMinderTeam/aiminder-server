package ai.aiminder.aiminderserver.assistant.repository

import ai.aiminder.aiminderserver.domain.Goal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class GoalRepository {
    private val database = mutableSetOf<Goal>()
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun save(goal: Goal): Goal {
        logger.info("Saving goal: $goal")
        database.add(goal)
        return goal
    }
}
