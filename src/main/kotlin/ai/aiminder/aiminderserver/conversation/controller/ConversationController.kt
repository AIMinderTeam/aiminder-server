package ai.aiminder.aiminderserver.conversation.controller

import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.conversation.dto.ConversationResponse
import ai.aiminder.aiminderserver.conversation.dto.DeleteConversationRequestDto
import ai.aiminder.aiminderserver.conversation.dto.GetConversationRequestDto
import ai.aiminder.aiminderserver.conversation.service.ConversationService
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/conversations")
class ConversationController(
  private val conversationService: ConversationService,
) : ConversationControllerDocs {
  @GetMapping
  override suspend fun getConversations(
    pageable: PageableRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<List<ConversationResponse>> {
    val dto = GetConversationRequestDto.from(user, pageable)
    val conversations: Page<ConversationResponse> = conversationService.get(dto)
    return ServiceResponse.from(conversations)
  }

  @DeleteMapping("/{conversationId}")
  override suspend fun deleteConversation(
    @PathVariable
    conversationId: UUID,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<String> {
    val dto = DeleteConversationRequestDto(conversationId, user.id)
    conversationService.delete(dto)
    return ServiceResponse.from("Conversation deleted successfully")
  }
}
