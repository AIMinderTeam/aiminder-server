package ai.aiminder.aiminderserver.domain

import ai.aiminder.aiminderserver.dto.GoalMilestone
import java.time.LocalDate

data class GoalDraft(
    val goalTitle: String,
    val goalTargetDate: LocalDate,
    val goalDescription: String,
    val milestones: List<GoalMilestone>,
)
