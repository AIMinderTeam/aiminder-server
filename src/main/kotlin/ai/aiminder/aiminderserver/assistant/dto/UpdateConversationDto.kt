package ai.aiminder.aiminderserver.assistant.dto

import java.util.UUID

data class UpdateConversationDto(
  val conversationId: UUID,
  val goalId: UUID,
)
