package ai.aiminder.aiminderserver.conversation.dto

import java.util.UUID

data class DeleteConversationRequestDto(
  val conversationId: UUID,
  val userId: UUID,
)
