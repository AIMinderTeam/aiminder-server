package ai.aiminder.aiminderserver.assistant.domain

import ai.aiminder.aiminderserver.assistant.entity.ChatEntity
import java.time.Instant
import java.util.UUID

data class Chat(
  val id: Long,
  val conversationId: UUID,
  val content: String,
  val type: ChatType,
  val createdAt: Instant,
) {
  companion object {
    fun from(entity: ChatEntity): Chat =
      Chat(
        id = entity.id!!,
        conversationId = entity.conversationId,
        content = entity.content,
        type = entity.type,
        createdAt = entity.createdAt,
      )
  }
}
