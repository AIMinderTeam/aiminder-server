package ai.aiminder.aiminderserver.assistant.dto

import java.time.LocalDate

data class GoalMilestone(
  val targetDate: LocalDate,
  val goal: String,
)
