package ai.aiminder.aiminderserver.schedule.repository.row

import java.time.LocalDateTime
import java.util.UUID

data class ScheduleWithGoalRow(
  val scheduleId: UUID,
  val scheduleTitle: String,
  val scheduleDescription: String?,
  val startDate: LocalDateTime,
  val endDate: LocalDateTime,
  val scheduleStatus: String,
  val goalId: UUID,
  val goalTitle: String,
  val goalDescription: String?,
  val targetDate: LocalDateTime,
  val goalStatus: String,
  val imageId: UUID?,
)
