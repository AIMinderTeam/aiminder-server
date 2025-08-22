package ai.aiminder.aiminderserver.assistant.service

import ai.aiminder.aiminderserver.assistant.client.AssistantClient
import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequest
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AssistantService(
  private val assistantClient: AssistantClient,
) {
  suspend fun chat(
    conversationId: UUID,
    request: AssistantRequest,
  ): AssistantResponse = assistantClient.chat(conversationId, request)
}