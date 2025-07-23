package ai.aiminder.aiminderserver.domain

import java.time.LocalDateTime
import java.util.UUID

data class Goal(
    val id: UUID,
    val originalText: String,
    val smartText: String,
    val metric: String,
    val targetDate: LocalDateTime,
) {
    companion object {
        fun create(draft: GoalDraft): Goal =
            Goal(
                id = UUID.randomUUID(),
                originalText = draft.originalText,
                smartText = draft.smartText,
                metric = draft.metric,
                targetDate = draft.targetDate,
            )
    }
}
