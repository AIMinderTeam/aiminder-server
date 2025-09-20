package ai.aiminder.aiminderserver.schedule.repository.row

import ai.aiminder.aiminderserver.common.util.toUtcInstant
import ai.aiminder.aiminderserver.schedule.domain.ScheduleStatus
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

data class ScheduleRow(
  val scheduleId: UUID,
  val goalId: UUID,
  val userId: UUID,
  val title: String,
  val description: String?,
  val status: String,
  val startDate: LocalDateTime,
  val endDate: LocalDateTime,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  val deletedAt: LocalDateTime?,
) {
  fun toScheduleStatus(): ScheduleStatus = ScheduleStatus.valueOf(status)

  fun startDateToInstant(): Instant = startDate.toUtcInstant()

  fun endDateToInstant(): Instant = endDate.toUtcInstant()

  fun createdAtToInstant(): Instant = createdAt.toUtcInstant()

  fun updatedAtToInstant(): Instant = updatedAt.toUtcInstant()

  fun deletedAtToInstant(): Instant? = deletedAt?.toUtcInstant()
}
