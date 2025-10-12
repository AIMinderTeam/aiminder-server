package ai.aiminder.aiminderserver.notification.dto

import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.data.domain.Pageable
import java.util.UUID

data class GetNotificationsRequestDto(
  val userId: UUID,
  val pageable: Pageable,
) {
  companion object {
    fun from(
      user: User,
      pageableRequest: PageableRequest,
    ): GetNotificationsRequestDto =
      GetNotificationsRequestDto(
        userId = user.id,
        pageable = pageableRequest.toDomain(),
      )
  }
}
