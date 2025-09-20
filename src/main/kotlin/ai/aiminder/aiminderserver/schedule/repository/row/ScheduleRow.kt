package ai.aiminder.aiminderserver.schedule.repository.row

import ai.aiminder.aiminderserver.schedule.domain.ScheduleStatus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
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

  fun startDateToInstant(): Instant = startDate.toInstant(ZoneOffset.UTC)

  fun endDateToInstant(): Instant = endDate.toInstant(ZoneOffset.UTC)

  fun createdAtToInstant(): Instant = createdAt.toInstant(ZoneOffset.UTC)

  fun updatedAtToInstant(): Instant = updatedAt.toInstant(ZoneOffset.UTC)

  fun deletedAtToInstant(): Instant? = deletedAt?.toInstant(ZoneOffset.UTC)
}
