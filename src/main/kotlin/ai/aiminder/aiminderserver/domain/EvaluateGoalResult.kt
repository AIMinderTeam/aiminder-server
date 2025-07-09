package ai.aiminder.aiminderserver.domain

import com.fasterxml.jackson.annotation.JsonProperty

data class EvaluateGoalResult(
    @get:JsonProperty(required = true)
    val review: String,
    @get:JsonProperty(required = true)
    val specific: Boolean,
    @get:JsonProperty(required = true)
    val measurable: Boolean,
    @get:JsonProperty(required = true)
    val achievable: Boolean,
    @get:JsonProperty(required = true)
    val relevant: Boolean,
    @get:JsonProperty(required = true)
    val timeBound: Boolean,
    @get:JsonProperty(required = true)
    val improvedGoal: String,
    @get:JsonProperty(required = true)
    val improvementReason: String,
)
