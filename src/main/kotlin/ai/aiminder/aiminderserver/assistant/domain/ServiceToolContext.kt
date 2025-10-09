package ai.aiminder.aiminderserver.assistant.domain

import java.util.UUID

data class ServiceToolContext(
  val conversationId: UUID,
  val userId: UUID,
)
