package ai.aiminder.aiminderserver.assistant.domain

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.core.io.Resource

data class AssistantResponseDto(
  @get:JsonProperty(required = true)
  val responses: List<AssistantResponsePayload>,
) {
  companion object {
    fun from(welcomeMessage: Resource): AssistantResponseDto =
      AssistantResponseDto(
        listOf(
          AssistantResponsePayload(
            AssistantResponseType.TEXT,
            listOf(welcomeMessage.getContentAsString(Charsets.UTF_8)),
          ),
          AssistantResponsePayload(
            AssistantResponseType.QUICK_REPLIES,
            listOf("다이어트 💪", "경제적 자유 💰", "자격증 취득 🏅"),
          ),
        ),
      )
  }
}
