package ai.aiminder.aiminderserver.goal.dto

import ai.aiminder.aiminderserver.user.domain.User
import java.util.UUID

data class GetGoalDetailRequestDto(
  val goalId: UUID,
  val userId: UUID,
) {
  companion object {
    fun from(
      goalId: UUID,
      user: User,
    ): GetGoalDetailRequestDto =
      GetGoalDetailRequestDto(
        goalId = goalId,
        userId = user.id,
      )
  }
}
