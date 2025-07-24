package ai.aiminder.aiminderserver.service

import ai.aiminder.aiminderserver.client.AssistantClient
import ai.aiminder.aiminderserver.domain.AssistantResponse
import ai.aiminder.aiminderserver.dto.AssistantRequest
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class AssistantService(
    private val assistantClient: AssistantClient,
) {
    suspend fun chat(request: AssistantRequest): AssistantResponse = assistantClient.chat(request)

    suspend fun chat(
        conversationId: String,
        userMessage: String,
    ): Flow<String> = assistantClient.chat(conversationId, userMessage)
}
