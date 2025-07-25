package ai.aiminder.aiminderserver.dto

import java.time.Instant

data class GoalMilestone(
    val targetDate: Instant,
    val goal: String,
)
