package ai.aiminder.aiminderserver.assistant.domain

import com.fasterxml.jackson.annotation.JsonProperty

data class AssistantResponseDto(
  @get:JsonProperty(required = true)
  val type: AssistantResponseType,
  @get:JsonProperty(required = true)
  val messages: List<String>,
)