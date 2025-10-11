package ai.aiminder.aiminderserver.conversation.dto

import ai.aiminder.aiminderserver.common.util.toUtcInstant
import ai.aiminder.aiminderserver.conversation.repository.row.ConversationRow
import java.time.Instant
import java.util.UUID

data class ConversationResponse(
  val conversationId: UUID,
  val recentChat: String,
  val recentAt: Instant,
  val goalId: UUID?,
  val goalTitle: String?,
) {
  companion object {
    fun fromRow(row: ConversationRow): ConversationResponse =
      ConversationResponse(
        conversationId = row.conversationId,
        recentChat = row.recentChat,
        recentAt = row.recentAt.toUtcInstant(),
        goalId = row.goalId,
        goalTitle = row.goalTitle,
      )
  }
}
