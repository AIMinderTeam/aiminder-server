package ai.aiminder.aiminderserver.domain

import com.fasterxml.jackson.annotation.JsonProperty

data class EvaluateGoalResult(
    @JsonProperty(value = "review", required = true)
    val review: String,
    @JsonProperty(value = "specific", required = true)
    val specific: Boolean,
    @JsonProperty(value = "measurable", required = true)
    val measurable: Boolean,
    @JsonProperty(value = "achievable", required = true)
    val achievable: Boolean,
    @JsonProperty(value = "relevant", required = true)
    val relevant: Boolean,
    @JsonProperty(value = "timeBound", required = true)
    val timeBound: Boolean,
    @JsonProperty(value = "isSMART", required = true)
    val isSMART: Boolean,
    @JsonProperty(value = "improvedGoal", required = true)
    val improvedGoal: String,
)
