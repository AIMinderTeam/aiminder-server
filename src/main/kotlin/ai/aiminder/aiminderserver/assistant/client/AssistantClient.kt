package ai.aiminder.aiminderserver.assistant.client

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponseDto
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequest
import java.util.UUID

interface AssistantClient {
  suspend fun chat(
    conversationId: UUID,
    assistantRequest: AssistantRequest,
  ): AssistantResponseDto
}
