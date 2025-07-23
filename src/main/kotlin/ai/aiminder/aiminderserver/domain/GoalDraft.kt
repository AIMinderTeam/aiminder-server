package ai.aiminder.aiminderserver.domain

import java.time.LocalDateTime

data class GoalDraft(
    val originalText: String,
    val smartText: String,
    val metric: String,
    val targetDate: LocalDateTime,
)
