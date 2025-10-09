package ai.aiminder.aiminderserver.assistant.service

import ai.aiminder.aiminderserver.assistant.client.AssistantClient
import ai.aiminder.aiminderserver.assistant.domain.AssistantResponseDto
import ai.aiminder.aiminderserver.assistant.domain.Conversation
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AssistantService(
  @param:Value("classpath:/prompts/welcome_message.txt")
  private val welcomeMessageResource: Resource,
  private val assistantClient: AssistantClient,
  private val chatMemory: ChatMemory,
  objectMapper: ObjectMapper,
) {
  private val assistantResponseDto: AssistantResponseDto = AssistantResponseDto.from(welcomeMessageResource)
  private val welcomeMessage: String = objectMapper.writeValueAsString(assistantResponseDto)
  private val assistantMessage = AssistantMessage(welcomeMessage)

  suspend fun sendMessage(
    conversationId: UUID,
    request: AssistantRequest,
  ): AssistantResponseDto = assistantClient.chat(conversationId, request)

  suspend fun startChat(conversation: Conversation): AssistantResponseDto {
    chatMemory.add(conversation.id.toString(), assistantMessage)
    return assistantResponseDto
  }
}
