package ai.aiminder.aiminderserver.domain

import com.fasterxml.jackson.annotation.JsonProperty

data class EvaluateGoalResult(
    @get:JsonProperty(value = "review", required = true)
    val review: String,
    @get:JsonProperty(value = "specific", required = true)
    val specific: Boolean,
    @get:JsonProperty(value = "measurable", required = true)
    val measurable: Boolean,
    @get:JsonProperty(value = "achievable", required = true)
    val achievable: Boolean,
    @get:JsonProperty(value = "relevant", required = true)
    val relevant: Boolean,
    @get:JsonProperty(value = "timeBound", required = true)
    val timeBound: Boolean,
    @get:JsonProperty(value = "improvedGoal", required = true)
    val improvedGoal: String,
    @get:JsonProperty(value = "improvementReason", required = true)
    val improvementReason: String,
)
