package ai.aiminder.aiminderserver.assistant.client

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponseDto
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequestDto

interface AssistantClient {
  suspend fun chat(dto: AssistantRequestDto): AssistantResponseDto
}
