package ai.aiminder.aiminderserver.assistant.domain

import com.fasterxml.jackson.annotation.JsonProperty

data class AssistantResponse(
  @get:JsonProperty(required = true)
  val responses: List<AssistantResponseDto>,
)
