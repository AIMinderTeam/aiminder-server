package ai.aiminder.aiminderserver.repository

import ai.aiminder.aiminderserver.domain.Goal
import org.springframework.stereotype.Repository

@Repository
class GoalRepository {
    val database = mutableSetOf<Goal>()

    fun save(goal: Goal): Goal {
        database.add(goal)
        return goal
    }
}
