package ai.aiminder.aiminderserver.assistant.entity

import ai.aiminder.aiminderserver.assistant.domain.ChatType
import ai.aiminder.aiminderserver.assistant.dto.ChatResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("chat")
data class ChatEntity(
  @Id
  @Column("chat_id")
  @get:JvmName("chatId")
  val id: Long? = null,
  val conversationId: UUID,
  val content: String,
  val type: ChatType,
  val createdAt: Instant = Instant.now(),
) : Persistable<Long> {
  override fun getId(): Long? = id

  override fun isNew(): Boolean = id == null

  companion object {
    fun from(
      chatResponse: ChatResponse,
      objectMapper: ObjectMapper,
    ): ChatEntity =
      ChatEntity(
        conversationId = chatResponse.conversationId,
        content = objectMapper.writeValueAsString(chatResponse.chat),
        type = chatResponse.chatType,
      )
  }
}
