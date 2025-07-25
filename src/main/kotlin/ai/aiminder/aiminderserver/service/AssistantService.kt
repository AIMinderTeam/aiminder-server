package ai.aiminder.aiminderserver.service

import ai.aiminder.aiminderserver.client.AssistantClient
import ai.aiminder.aiminderserver.domain.AssistantResponse
import ai.aiminder.aiminderserver.dto.AssistantRequest
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
