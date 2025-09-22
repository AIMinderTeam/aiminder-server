package ai.aiminder.aiminderserver.assistant.service

import ai.aiminder.aiminderserver.assistant.client.AssistantClient
import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.domain.Conversation
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequest
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AssistantService(
  @param:Value("classpath:/prompts/welcome_message.txt")
  private val welcomeMessage: Resource,
  private val assistantClient: AssistantClient,
  private val chatMemory: ChatMemory,
) {
  suspend fun sendMessage(
    conversationId: UUID,
    request: AssistantRequest,
  ): AssistantResponse = assistantClient.chat(conversationId, request)

  suspend fun startChat(conversation: Conversation): AssistantResponse {
    val assistantResponse = AssistantResponse.from(welcomeMessage)
    val message: String = assistantResponse.toString()
    val assistantMessage = AssistantMessage(message)
    chatMemory.add(conversation.id.toString(), assistantMessage)
    return assistantResponse
  }
}
