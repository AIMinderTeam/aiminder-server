package ai.aiminder.aiminderserver.goal.dto

import java.util.UUID

data class DeleteGoalRequestDto(
  val goalId: UUID,
  val userId: UUID,
)
