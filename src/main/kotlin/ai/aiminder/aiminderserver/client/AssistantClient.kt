package ai.aiminder.aiminderserver.client

import ai.aiminder.aiminderserver.domain.AssistantResponse
import ai.aiminder.aiminderserver.dto.AssistantRequest
import kotlinx.coroutines.flow.Flow

interface AssistantClient {
    suspend fun chat(assistantRequest: AssistantRequest): AssistantResponse

    suspend fun chat(
        conversationId: String,
        userMessage: String,
    ): Flow<String>
}
