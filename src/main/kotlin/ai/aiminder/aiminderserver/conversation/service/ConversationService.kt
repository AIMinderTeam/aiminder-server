package ai.aiminder.aiminderserver.conversation.service

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.domain.AssistantResponseType
import ai.aiminder.aiminderserver.assistant.domain.ChatResponseDto
import ai.aiminder.aiminderserver.assistant.domain.ChatRow
import ai.aiminder.aiminderserver.assistant.domain.ChatType
import ai.aiminder.aiminderserver.assistant.dto.ChatResponse
import ai.aiminder.aiminderserver.assistant.dto.UpdateConversationDto
import ai.aiminder.aiminderserver.assistant.error.AssistantError
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.conversation.domain.Conversation
import ai.aiminder.aiminderserver.conversation.dto.ConversationResponse
import ai.aiminder.aiminderserver.conversation.dto.GetConversationRequestDto
import ai.aiminder.aiminderserver.conversation.dto.GetMessagesRequestDto
import ai.aiminder.aiminderserver.conversation.entity.ConversationEntity
import ai.aiminder.aiminderserver.conversation.repository.ConversationQueryRepository
import ai.aiminder.aiminderserver.conversation.repository.ConversationRepository
import ai.aiminder.aiminderserver.conversation.repository.row.ConversationRow
import ai.aiminder.aiminderserver.user.domain.User
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
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
  private val objectMapper: ObjectMapper,
) {
  suspend fun create(user: User): Conversation =
    ConversationEntity
      .from(user)
      .let { conversationRepository.save(it) }
      .let { Conversation.from(it) }

  suspend fun findById(conversationId: UUID): Conversation =
    conversationRepository
      .findById(conversationId)
      ?.takeIf { it.deletedAt == null }
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
    val conversations: Flow<ConversationResponse> =
      conversationQueryRepository
        .findConversationsBy(dto)
        .map { conversation -> formatRecentChat(conversation) }
        .map { ConversationResponse.fromRow(it) }

    val totalCount = conversationQueryRepository.countBy(dto)

    return PageImpl(conversations.toList(), dto.pageable, totalCount)
  }

  suspend fun get(dto: GetMessagesRequestDto): Page<ChatResponse> {
    val chatRows: Flow<ChatResponse> =
      conversationQueryRepository
        .findChatBy(dto)
        .map { row -> formatChat(row, dto.conversationId) }

    val totalCount = conversationQueryRepository.countBy(dto)

    return PageImpl(chatRows.toList(), dto.pageable, totalCount)
  }

  private fun formatRecentChat(conversation: ConversationRow): ConversationRow {
    val recentChat =
      when (conversation.type) {
        null -> ""
        else ->
          when (ChatType.valueOf(conversation.type)) {
            ChatType.ASSISTANT ->
              objectMapper
                .readValue(conversation.recentChat, AssistantResponse::class.java)
                .responses
                .firstOrNull { it.type == AssistantResponseType.TEXT }
                ?.messages
                ?.firstOrNull()
                ?: ""

            ChatType.USER -> conversation.recentChat
          }
      }
    return conversation.copy(recentChat = recentChat)
  }

  private fun formatChat(
    chat: ChatRow,
    conversationId: UUID,
  ): ChatResponse {
    val chatResponses: List<ChatResponseDto> =
      when (ChatType.valueOf(chat.type)) {
        ChatType.ASSISTANT ->
          objectMapper
            .readValue(chat.content, AssistantResponse::class.java)
            .responses

        ChatType.USER ->
          listOf(ChatResponseDto(AssistantResponseType.TEXT, listOf(chat.content)))
      }
    return ChatResponse(conversationId, chatResponses, ChatType.valueOf(chat.type))
  }
}
