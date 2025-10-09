package ai.aiminder.aiminderserver.user.dto

import ai.aiminder.aiminderserver.user.domain.User
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "사용자 정보 응답 데이터")
data class GetUserResponse(
  @Schema(description = "사용자 고유 ID", example = "2f6a3a4c-1c3b-4bde-9d2a-6c2c8b6a1e7f")
  val id: UUID,
) {
  companion object {
    fun from(user: User): GetUserResponse =
      GetUserResponse(
        id = user.id,
      )
  }
}
