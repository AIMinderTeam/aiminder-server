package ai.aiminder.aiminderserver.conversation.domain

import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.conversation.dto.DeleteConversationRequestDto
import ai.aiminder.aiminderserver.conversation.entity.ConversationEntity
import java.time.Instant
import java.util.UUID

data class Conversation(
  val id: UUID,
  val userId: UUID,
  val goalId: UUID?,
  val createdAt: Instant,
  val deletedAt: Instant?,
) {
  fun update(goalId: UUID): Conversation = this.copy(goalId = goalId)

  fun delete(dto: DeleteConversationRequestDto): Conversation {
    if (userId != dto.userId) throw AuthError.Unauthorized()
    return copy(deletedAt = Instant.now())
  }

  companion object {
    fun from(conversationEntity: ConversationEntity): Conversation =
      Conversation(
        id = conversationEntity.id!!,
        userId = conversationEntity.userId,
        goalId = conversationEntity.goalId,
        createdAt = conversationEntity.createdAt,
        deletedAt = conversationEntity.deletedAt,
      )
  }
}
