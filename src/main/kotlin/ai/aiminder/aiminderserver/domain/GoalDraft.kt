package ai.aiminder.aiminderserver.domain

import ai.aiminder.aiminderserver.dto.GoalMilestone
import java.time.Instant

data class GoalDraft(
    val goalTitle: String,
    val goalTargetDate: Instant,
    val goalDescription: String,
    val milestones: List<GoalMilestone>,
)
