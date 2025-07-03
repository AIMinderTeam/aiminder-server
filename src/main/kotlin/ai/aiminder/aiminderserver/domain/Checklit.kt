package ai.aiminder.aiminderserver.domain

data class Checklist(
    val specific: Boolean,
    val measurable: Boolean,
    val achievable: Boolean,
    val relevant: Boolean,
    val timeBound: Boolean,
)
