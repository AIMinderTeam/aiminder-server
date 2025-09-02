package ai.aiminder.aiminderserver.goal.dto

import java.time.Instant

data class CreateGoalRequest(
  val title: String,
  val description: String,
  val targetDate: Instant,
)