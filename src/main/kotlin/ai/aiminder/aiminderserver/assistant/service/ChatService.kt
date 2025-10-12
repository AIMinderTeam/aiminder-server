package ai.aiminder.aiminderserver.assistant.service

import ai.aiminder.aiminderserver.assistant.domain.Chat
import ai.aiminder.aiminderserver.assistant.dto.ChatResponse
import ai.aiminder.aiminderserver.assistant.entity.ChatEntity
import ai.aiminder.aiminderserver.assistant.repository.ChatRepository
import com.fasterxml.jackson.databind.ObjectMapper
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
}
