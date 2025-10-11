package ai.aiminder.aiminderserver.conversation.dto

import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.data.domain.Pageable
import java.util.UUID

data class GetConversationRequestDto(
  val userId: UUID,
  val pageable: Pageable,
) {
  companion object {
    fun from(
      user: User,
      pageableRequest: PageableRequest,
    ): GetConversationRequestDto =
      GetConversationRequestDto(
        userId = user.id,
        pageable = pageableRequest.toDomain(),
      )
  }
}
