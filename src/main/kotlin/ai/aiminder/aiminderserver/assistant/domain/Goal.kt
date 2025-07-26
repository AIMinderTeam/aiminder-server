package ai.aiminder.aiminderserver.assistant.domain

import ai.aiminder.aiminderserver.dto.GoalMilestone
import java.time.LocalDate
import java.util.UUID

data class Goal(
    val id: UUID,
    val goalTitle: String,
    val goalTargetDate: LocalDate,
    val goalDescription: String,
    val milestones: List<GoalMilestone>,
) {
    companion object {
        fun create(draft: GoalDraft): Goal =
            Goal(
                id = UUID.randomUUID(),
                goalTitle = draft.goalTitle,
                goalTargetDate = draft.goalTargetDate,
                goalDescription = draft.goalDescription,
                milestones = draft.milestones,
            )
    }
}
