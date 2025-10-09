package ai.aiminder.aiminderserver.assistant.service

import ai.aiminder.aiminderserver.assistant.domain.Conversation
import ai.aiminder.aiminderserver.assistant.entity.ConversationEntity
import ai.aiminder.aiminderserver.assistant.error.AssistantError
import ai.aiminder.aiminderserver.assistant.repository.ConversationRepository
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ConversationService(
  private val conversationRepository: ConversationRepository,
) {
  suspend fun create(user: User): Conversation =
    ConversationEntity
      .from(user)
      .let { conversationRepository.save(it) }
      .let { Conversation.from(it) }

  suspend fun findById(conversationId: UUID): Conversation =
    conversationRepository
      .findById(conversationId)
      ?.let { Conversation.from(it) }
      ?: throw AssistantError.ConversationNotFound(conversationId.toString())

  suspend fun validateUserAuthorization(
    conversationId: UUID,
    user: User,
  ) {
    val conversation = findById(conversationId)
    if (conversation.userId != user.id) {
      throw AuthError.Unauthorized()
    }
  }
}
