package ai.aiminder.aiminderserver.assistant.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "AI 어시스턴트 응답 타입")
enum class AssistantResponseType {
  @Schema(description = "텍스트 응답")
  TEXT,

  @Schema(description = "빠른 답변 버튼")
  QUICK_REPLIES,
}
