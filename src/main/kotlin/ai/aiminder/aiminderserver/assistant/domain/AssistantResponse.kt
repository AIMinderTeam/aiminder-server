package ai.aiminder.aiminderserver.assistant.domain

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.core.io.Resource

data class AssistantResponse(
  @get:JsonProperty(required = true)
  val responses: List<ChatResponseDto>,
) {
  companion object {
    fun from(welcomeMessage: Resource): AssistantResponse =
      AssistantResponse(
        listOf(
          ChatResponseDto(
            AssistantResponseType.TEXT,
            listOf(welcomeMessage.getContentAsString(Charsets.UTF_8)),
          ),
          ChatResponseDto(
            AssistantResponseType.QUICK_REPLIES,
            listOf("다이어트 💪", "경제적 자유 💰", "자격증 취득 🏅"),
          ),
        ),
      )

    fun from(message: String): AssistantResponse =
      AssistantResponse(listOf(ChatResponseDto(AssistantResponseType.TEXT, listOf(message))))
  }
}
