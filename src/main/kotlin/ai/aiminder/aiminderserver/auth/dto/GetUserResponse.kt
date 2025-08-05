package ai.aiminder.aiminderserver.auth.dto

import ai.aiminder.aiminderserver.auth.domain.User
import java.util.UUID

data class GetUserResponse(
  val id: UUID,
) {
  companion object {
    fun from(user: User): GetUserResponse =
      GetUserResponse(
        id = user.id!!,
      )
  }
}