package ai.aiminder.aiminderserver.auth.dto

import ai.aiminder.aiminderserver.auth.entity.UserEntity
import java.util.UUID

data class GetUserResponse(
  val id: UUID,
) {
  companion object {
    fun from(userEntity: UserEntity): GetUserResponse =
      GetUserResponse(
        id = userEntity.id!!,
      )
  }
}