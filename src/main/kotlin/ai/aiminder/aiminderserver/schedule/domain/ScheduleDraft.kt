package ai.aiminder.aiminderserver.schedule.domain

import java.time.LocalDate

data class ScheduleDraft(
  val date: LocalDate,
  val task: String,
)
