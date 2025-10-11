package ai.aiminder.aiminderserver.conversation.repository.row

import java.time.LocalDateTime
import java.util.UUID

data class ConversationRow(
  val conversationId: UUID,
  val recentChat: String,
  val recentAt: LocalDateTime?,
  val goalId: UUID?,
  val goalTitle: String?,
)
