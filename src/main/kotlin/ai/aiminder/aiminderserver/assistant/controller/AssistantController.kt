package ai.aiminder.aiminderserver.assistant.controller

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequest
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequestDto
import ai.aiminder.aiminderserver.assistant.dto.ChatResponse
import ai.aiminder.aiminderserver.assistant.service.AssistantService
import ai.aiminder.aiminderserver.common.error.CommonError
import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.conversation.domain.Conversation
import ai.aiminder.aiminderserver.conversation.dto.GetMessagesRequestDto
import ai.aiminder.aiminderserver.conversation.service.ConversationService
import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.service.GoalService
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/conversations")
class AssistantController(
  private val assistantService: AssistantService,
  private val conversationService: ConversationService,
  private val goalService: GoalService,
) : AssistantControllerDocs {
  @Transactional
  @PostMapping("/chat")
  override suspend fun startChat(
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<ChatResponse> {
    val conversation: Conversation = conversationService.create(user)
    val assistantResponse: AssistantResponse = assistantService.startChat(conversation)
    val response: ChatResponse =
      ChatResponse.from(
        conversation = conversation,
        assistantResponse = assistantResponse,
      )
    return ServiceResponse.from(response)
  }

  @Transactional
  @PostMapping("/{conversationId}/chat")
  override suspend fun sendMessage(
    @PathVariable
    conversationId: UUID,
    @RequestBody
    request: AssistantRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<ChatResponse> {
    if (request.text.isBlank()) {
      throw CommonError.InvalidRequest("메시지 내용이 비어있습니다.")
    }
    conversationService.validateUserAuthorization(conversationId, user)
    val conversation: Conversation = conversationService.findById(conversationId)
    val goal: Goal? = conversation.goalId?.let { goalService.get(it) }
    val requestDto: AssistantRequestDto = AssistantRequestDto.from(conversationId, user, request, goal)
    val assistantResponse: AssistantResponse = assistantService.sendMessage(requestDto)
    val response: ChatResponse =
      ChatResponse.from(
        conversation = conversation,
        assistantResponse = assistantResponse,
      )
    return ServiceResponse.from(response)
  }

  @GetMapping("/{conversationId}/chat")
  override suspend fun getMessages(
    @PathVariable
    conversationId: UUID,
    pageable: PageableRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<List<ChatResponse>> {
    conversationService.validateUserAuthorization(conversationId, user)
    val dto = GetMessagesRequestDto.from(conversationId, pageable)
    val messages: Page<ChatResponse> = conversationService.get(dto)
    return ServiceResponse.from(messages)
  }
}
