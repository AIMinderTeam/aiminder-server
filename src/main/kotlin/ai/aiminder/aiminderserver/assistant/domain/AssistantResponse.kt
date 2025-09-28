package ai.aiminder.aiminderserver.assistant.domain

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.core.io.Resource

data class AssistantResponse(
  @get:JsonProperty(required = true)
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
