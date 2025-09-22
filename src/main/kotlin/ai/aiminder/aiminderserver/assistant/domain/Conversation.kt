package ai.aiminder.aiminderserver.assistant.domain

import ai.aiminder.aiminderserver.assistant.entity.ConversationEntity
import java.time.Instant
import java.util.UUID

data class Conversation(
  val id: UUID,
  val userId: UUID,
  val createdAt: Instant,
  val deletedAt: Instant?,
) {
  companion object {
    fun from(conversationEntity: ConversationEntity): Conversation =
      Conversation(
        id = conversationEntity.id!!,
        userId = conversationEntity.userId,
        createdAt = conversationEntity.createdAt,
        deletedAt = conversationEntity.deletedAt,
      )
  }
}
