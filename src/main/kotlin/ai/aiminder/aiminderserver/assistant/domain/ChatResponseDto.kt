package ai.aiminder.aiminderserver.assistant.domain

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "AI 어시스턴트 개별 응답 데이터")
data class ChatResponseDto(
  @get:JsonProperty(required = true)
  @Schema(description = "응답 타입", example = "TEXT")
  val type: AssistantResponseType,
  @get:JsonProperty(required = true)
  @Schema(description = "응답 메시지 배열", example = "[\"안녕하세요!\", \"무엇을 도와드릴까요?\"]")
  val messages: List<String>,
)
