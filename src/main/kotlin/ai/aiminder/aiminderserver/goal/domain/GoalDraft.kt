package ai.aiminder.aiminderserver.goal.domain

import ai.aiminder.aiminderserver.assistant.dto.GoalMilestone
import java.time.LocalDate

data class GoalDraft(
  val goalTitle: String,
  val goalTargetDate: LocalDate,
  val goalDescription: String,
  val milestones: List<GoalMilestone>,
) {
  val description: String
    get() = "$goalDescription\n\n$descriptionWithMilestones"

  val descriptionWithMilestones: String
    get() = milestones.joinToString("\n") { "- 목표 : ${it.goal}, 목표 날짜 : ${it.targetDate}" }
}
