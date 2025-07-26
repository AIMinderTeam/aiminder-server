package ai.aiminder.aiminderserver.assistant.controller

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequest
import ai.aiminder.aiminderserver.assistant.service.AssistantService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AssistantController(
    private val assistantService: AssistantService,
) {
    @PostMapping("/chat/{conversationId}")
    suspend fun chat(
        @PathVariable
        conversationId: UUID,
        @RequestBody
        request: AssistantRequest,
    ): AssistantResponse = assistantService.chat(conversationId, request)
}
