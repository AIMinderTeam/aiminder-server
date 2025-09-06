package ai.aiminder.aiminderserver.user.dto

import ai.aiminder.aiminderserver.user.domain.User
import java.util.UUID

data class GetUserResponse(
  val id: UUID,
) {
  companion object {
    fun from(user: User): GetUserResponse =
      GetUserResponse(
        id = user.id,
      )
  }
}
