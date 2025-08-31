package ai.aiminder.aiminderserver.assistant.domain

import java.time.LocalDate

data class AiSchedule(
  val date: LocalDate,
  val task: String,
)