package ai.aiminder.aiminderserver.conversation.controller

import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.conversation.dto.ConversationResponse
import ai.aiminder.aiminderserver.conversation.dto.GetConversationRequestDto
import ai.aiminder.aiminderserver.conversation.service.ConversationService
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/conversations")
class ConversationController(
  private val conversationService: ConversationService,
) {
  @GetMapping
  suspend fun getConversations(
    pageable: PageableRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<List<ConversationResponse>> {
    val dto = GetConversationRequestDto.from(user, pageable)
    val conversations: Page<ConversationResponse> = conversationService.get(dto)
    return ServiceResponse.from(conversations)
  }
}
