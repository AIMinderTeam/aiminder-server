package ai.aiminder.aiminderserver.assistant.domain

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.core.io.Resource

@Schema(description = "AI 어시스턴트 응답 데이터")
data class AssistantResponse(
  @get:JsonProperty(required = true)
  @Schema(description = "응답 메시지 목록", example = "[{\"type\": \"TEXT\", \"messages\": [\"안녕하세요!\"]}]")
  val responses: List<AssistantResponseDto>,
) {
  companion object {
    fun from(welcomeMessage: Resource): AssistantResponse =
      AssistantResponse(
        listOf(
          AssistantResponseDto(
            AssistantResponseType.TEXT,
            listOf(welcomeMessage.getContentAsString(Charsets.UTF_8)),
          ),
          AssistantResponseDto(
            AssistantResponseType.QUICK_REPLIES,
            listOf("다이어트 💪", "경제적 자유 💰", "자격증 취득 🏅"),
          ),
        ),
      )
  }
}
