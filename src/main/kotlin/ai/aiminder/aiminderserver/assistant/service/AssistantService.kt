package ai.aiminder.aiminderserver.assistant.service

import ai.aiminder.aiminderserver.assistant.client.AssistantClient
import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequestDto
import ai.aiminder.aiminderserver.conversation.domain.Conversation
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service

@Service
class AssistantService(
  @param:Value("classpath:/prompts/welcome_message.txt")
  private val welcomeMessageResource: Resource,
  private val assistantClient: AssistantClient,
  private val chatMemory: ChatMemory,
  objectMapper: ObjectMapper,
) {
  private val assistantResponse: AssistantResponse = AssistantResponse.from(welcomeMessageResource)
  private val welcomeMessage: String = objectMapper.writeValueAsString(assistantResponse)
  private val assistantMessage = AssistantMessage(welcomeMessage)

  suspend fun sendMessage(dto: AssistantRequestDto): AssistantResponse = assistantClient.chat(dto)

  suspend fun startChat(conversation: Conversation): AssistantResponse {
    chatMemory.add(conversation.id.toString(), assistantMessage)
    return assistantResponse
  }
}
