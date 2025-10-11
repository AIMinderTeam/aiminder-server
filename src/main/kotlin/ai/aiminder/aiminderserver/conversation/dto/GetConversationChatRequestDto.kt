package ai.aiminder.aiminderserver.conversation.dto

import ai.aiminder.aiminderserver.common.request.PageableRequest
import org.springframework.data.domain.Pageable
import java.util.UUID

data class GetConversationChatRequestDto(
  val conversationId: UUID,
  val pageable: Pageable,
) {
  companion object {
    fun from(
      conversationId: UUID,
      pageableRequest: PageableRequest,
    ): GetConversationChatRequestDto =
      GetConversationChatRequestDto(
        conversationId = conversationId,
        pageable = pageableRequest.toDomain(),
      )
  }
}
