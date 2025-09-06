package ai.aiminder.aiminderserver.goal.dto

import java.time.Instant
import java.util.UUID

data class CreateGoalRequest(
  val title: String,
  val description: String,
  val targetDate: Instant,
)

data class CreateGoalRequestDto(
  val userId: UUID,
  val title: String,
  val description: String,
  val targetDate: Instant,
)
