package ai.aiminder.aiminderserver.controller

import ai.aiminder.aiminderserver.domain.AssistantResponse
import ai.aiminder.aiminderserver.dto.AssistantRequest
import ai.aiminder.aiminderserver.service.AssistantService
import kotlinx.coroutines.flow.Flow
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AssistantController(
    private val assistantService: AssistantService,
) {
    @PostMapping("/chat")
    suspend fun chat(
        @RequestBody
        request: AssistantRequest,
    ): AssistantResponse = assistantService.chat(request)

    @PostMapping("/chat/{conversationId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    suspend fun chat(
        @PathVariable
        conversationId: String,
        @RequestBody
        message: String,
    ): Flow<String> = assistantService.chat(conversationId, message)
}
