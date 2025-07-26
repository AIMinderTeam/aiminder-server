package ai.aiminder.aiminderserver.assistant.client

import ai.aiminder.aiminderserver.domain.AssistantResponse
import ai.aiminder.aiminderserver.dto.AssistantRequest
import java.util.UUID

interface AssistantClient {
    suspend fun chat(
        conversationId: UUID,
        assistantRequest: AssistantRequest,
    ): AssistantResponse
}
