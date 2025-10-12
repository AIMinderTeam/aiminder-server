package ai.aiminder.aiminderserver.assistant.dto

import ai.aiminder.aiminderserver.common.request.PageableRequest
import org.springframework.data.domain.Pageable
import java.util.UUID

data class GetMessagesRequestDto(
  val conversationId: UUID,
  val pageable: Pageable,
) {
  companion object {
    fun from(
      conversationId: UUID,
      pageableRequest: PageableRequest,
    ): GetMessagesRequestDto =
      GetMessagesRequestDto(
        conversationId = conversationId,
        pageable = pageableRequest.toDomain(),
      )
  }
}
