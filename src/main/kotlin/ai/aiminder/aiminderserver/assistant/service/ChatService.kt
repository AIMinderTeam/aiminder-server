package ai.aiminder.aiminderserver.assistant.service

import ai.aiminder.aiminderserver.assistant.domain.Chat
import ai.aiminder.aiminderserver.assistant.dto.ChatResponse
import ai.aiminder.aiminderserver.assistant.dto.GetMessagesRequestDto
import ai.aiminder.aiminderserver.assistant.entity.ChatEntity
import ai.aiminder.aiminderserver.assistant.repository.ChatRepository
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service

@Service
class ChatService(
  private val chatRepository: ChatRepository,
  private val objectMapper: ObjectMapper,
) {
  suspend fun create(chatResponse: ChatResponse): Chat =
    ChatEntity
      .from(chatResponse, objectMapper)
      .let { chatRepository.save(it) }
      .let { Chat.from(it) }

  suspend fun get(dto: GetMessagesRequestDto): Page<ChatResponse> {
    val chatResponses =
      chatRepository
        .findAllByConversationIdOrderByIdDesc(dto.conversationId, dto.pageable)
        .map { chat -> ChatResponse.from(chat, objectMapper) }

    val totalCount = chatRepository.countByConversationId(dto.conversationId)

    return PageImpl(chatResponses.toList().reversed(), dto.pageable, totalCount)
  }
}
