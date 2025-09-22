package ai.aiminder.aiminderserver.assistant.service

import ai.aiminder.aiminderserver.assistant.domain.Conversation
import ai.aiminder.aiminderserver.assistant.entity.ConversationEntity
import ai.aiminder.aiminderserver.assistant.repository.ConversationRepository
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.stereotype.Service

@Service
class ConversationService(
  private val conversationRepository: ConversationRepository,
) {
  suspend fun create(user: User): Conversation =
    ConversationEntity
      .from(user)
      .let { conversationRepository.save(it) }
      .let { Conversation.from(it) }
}
