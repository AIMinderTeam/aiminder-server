package ai.aiminder.aiminderserver.goal.dto

import ai.aiminder.aiminderserver.goal.domain.GoalStatus
import ai.aiminder.aiminderserver.user.domain.User
import java.time.Instant
import java.util.UUID

data class UpdateGoalRequest(
  val title: String? = null,
  val description: String? = null,
  val targetDate: Instant? = null,
  val imageId: UUID? = null,
  val status: GoalStatus? = null,
)

data class UpdateGoalRequestDto(
  val goalId: UUID,
  val userId: UUID,
  val title: String? = null,
  val description: String? = null,
  val targetDate: Instant? = null,
  val imageId: UUID? = null,
  val status: GoalStatus? = null,
) {
  companion object {
    fun from(
      goalId: UUID,
      request: UpdateGoalRequest,
      user: User,
    ): UpdateGoalRequestDto =
      UpdateGoalRequestDto(
        goalId = goalId,
        userId = user.id,
        title = request.title,
        description = request.description,
        targetDate = request.targetDate,
        imageId = request.imageId,
        status = request.status,
      )
  }
}
