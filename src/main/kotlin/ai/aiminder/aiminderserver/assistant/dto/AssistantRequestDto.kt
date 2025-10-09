package ai.aiminder.aiminderserver.assistant.dto

import ai.aiminder.aiminderserver.user.domain.User
import java.util.UUID

data class AssistantRequestDto(
  val conversationId: UUID,
  val userId: UUID,
  val text: String,
) {
  companion object {
    fun from(
      conversationId: UUID,
      user: User,
      request: AssistantRequest,
    ): AssistantRequestDto = AssistantRequestDto(conversationId = conversationId, userId = user.id, text = request.text)
  }
}
