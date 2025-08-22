package ai.aiminder.aiminderserver.assistant.domain

import ai.aiminder.aiminderserver.assistant.dto.GoalMilestone
import java.time.LocalDate

data class GoalDraft(
  val goalTitle: String,
  val goalTargetDate: LocalDate,
  val goalDescription: String,
  val milestones: List<GoalMilestone>,
)