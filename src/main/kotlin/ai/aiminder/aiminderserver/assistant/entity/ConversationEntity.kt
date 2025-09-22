package ai.aiminder.aiminderserver.assistant.entity

import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("conversations")
data class ConversationEntity(
  @Id
  @Column("conversation_id")
  @get:JvmName("conversationId")
  val id: UUID? = null,
  val userId: UUID,
  val createdAt: Instant = Instant.now(),
  val deletedAt: Instant? = null,
) : Persistable<UUID> {
  override fun getId(): UUID? = id

  override fun isNew(): Boolean = id == null

  companion object {
    fun from(user: User): ConversationEntity =
      ConversationEntity(
        userId = user.id,
      )
  }
}
