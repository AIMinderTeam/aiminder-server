package ai.aiminder.aiminderserver.domain

data class EvaluateGoalResult(
    val review: String,
    val specific: Boolean,
    val measurable: Boolean,
    val achievable: Boolean,
    val relevant: Boolean,
    val timeBound: Boolean,
    val isSMART: Boolean,
    val improvedGoal: String,
)
