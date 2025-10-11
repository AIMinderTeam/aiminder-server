package ai.aiminder.aiminderserver.conversation.service

import ai.aiminder.aiminderserver.assistant.dto.UpdateConversationDto
import ai.aiminder.aiminderserver.assistant.error.AssistantError
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.conversation.domain.Conversation
import ai.aiminder.aiminderserver.conversation.dto.ConversationResponse
import ai.aiminder.aiminderserver.conversation.dto.GetConversationRequestDto
import ai.aiminder.aiminderserver.conversation.entity.ConversationEntity
import ai.aiminder.aiminderserver.conversation.repository.ConversationQueryRepository
import ai.aiminder.aiminderserver.conversation.repository.ConversationRepository
import ai.aiminder.aiminderserver.user.domain.User
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ConversationService(
  private val conversationRepository: ConversationRepository,
  private val conversationQueryRepository: ConversationQueryRepository,
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

  suspend fun update(dto: UpdateConversationDto): Conversation =
    findById(dto.conversationId)
      .update(dto.goalId)
      .let { ConversationEntity.from(it) }
      .let { conversationRepository.save(it) }
      .let { Conversation.from(it) }

  suspend fun get(dto: GetConversationRequestDto): Page<ConversationResponse> {
    val conversations =
      conversationQueryRepository
        .findAllBy(dto)
        .map { ConversationResponse.fromRow(it) }

    val totalCount = conversationQueryRepository.countBy(dto)

    return PageImpl(conversations.toList(), dto.pageable, totalCount)
  }
}
