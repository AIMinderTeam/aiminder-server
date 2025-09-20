package ai.aiminder.aiminderserver.schedule.dto

import java.time.Instant
import java.util.UUID

data class CreateScheduleRequest(
  val goalId: UUID,
  val title: String,
  val description: String? = null,
  val startDate: Instant,
  val endDate: Instant,
)

data class CreateScheduleRequestDto(
  val goalId: UUID,
  val userId: UUID,
  val title: String,
  val description: String? = null,
  val startDate: Instant,
  val endDate: Instant,
)
