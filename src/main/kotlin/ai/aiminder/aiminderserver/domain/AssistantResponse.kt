package ai.aiminder.aiminderserver.domain

import com.fasterxml.jackson.annotation.JsonProperty

data class AssistantResponse(
    @get:JsonProperty(required = true)
    val responses: List<AssistantResponseDto>,
)
