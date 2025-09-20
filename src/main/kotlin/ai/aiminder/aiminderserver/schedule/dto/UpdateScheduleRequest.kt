package ai.aiminder.aiminderserver.schedule.dto

import ai.aiminder.aiminderserver.schedule.domain.ScheduleStatus
import java.time.Instant
import java.util.UUID

data class UpdateScheduleRequest(
  val title: String? = null,
  val content: String? = null,
  val status: ScheduleStatus? = null,
  val startDate: Instant? = null,
  val endDate: Instant? = null,
)

data class UpdateScheduleRequestDto(
  val id: UUID,
  val userId: UUID,
  val title: String? = null,
  val content: String? = null,
  val status: ScheduleStatus? = null,
  val startDate: Instant? = null,
  val endDate: Instant? = null,
)
