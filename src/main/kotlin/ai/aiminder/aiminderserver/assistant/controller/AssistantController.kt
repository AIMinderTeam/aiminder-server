package ai.aiminder.aiminderserver.assistant.controller

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.domain.Conversation
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequest
import ai.aiminder.aiminderserver.assistant.service.AssistantService
import ai.aiminder.aiminderserver.assistant.service.ConversationService
import ai.aiminder.aiminderserver.common.error.CommonError
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class AssistantController(
  private val assistantService: AssistantService,
  private val conversationService: ConversationService,
) : AssistantControllerDocs {
  @Transactional
  @PostMapping("/chat")
  override suspend fun startChat(
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<AssistantResponse> {
    val conversation: Conversation = conversationService.create(user)
    val assistantResponse: AssistantResponse = assistantService.startChat(conversation)
    return ServiceResponse.from(assistantResponse)
  }

  @PostMapping("/chat/{conversationId}")
  override suspend fun sendMessage(
    @PathVariable
    conversationId: UUID,
    @RequestBody
    request: AssistantRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<AssistantResponse> {
    if (request.text.isBlank()) {
      throw CommonError.InvalidRequest("메시지 내용이 비어있습니다.")
    }
    conversationService.validateUserAuthorization(conversationId, user)
    val assistantResponse: AssistantResponse = assistantService.sendMessage(conversationId, request)
    return ServiceResponse.from(assistantResponse)
  }
}
