package ai.aiminder.aiminderserver.domain

data class EvaluateGoalResult(
    val improved: String,
    val score: Int,
    val checklist: Checklist,
    val feedback: List<String>,
)
