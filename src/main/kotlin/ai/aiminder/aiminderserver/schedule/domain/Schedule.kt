package ai.aiminder.aiminderserver.schedule.domain

import ai.aiminder.aiminderserver.schedule.entity.ScheduleEntity
import ai.aiminder.aiminderserver.schedule.repository.row.ScheduleRow
import java.time.Instant
import java.util.UUID

data class Schedule(
  val id: UUID,
  val goalId: UUID,
  val userId: UUID,
  val title: String,
  val description: String?,
  val status: ScheduleStatus,
  val startDate: Instant,
  val endDate: Instant,
  val createdAt: Instant,
  val updatedAt: Instant,
  val deletedAt: Instant?,
) {
  companion object {
    fun fromEntity(scheduleEntity: ScheduleEntity): Schedule =
      Schedule(
        id = scheduleEntity.id!!,
        goalId = scheduleEntity.goalId,
        userId = scheduleEntity.userId,
        title = scheduleEntity.title,
        description = scheduleEntity.description,
        status = scheduleEntity.status,
        startDate = scheduleEntity.startDate,
        endDate = scheduleEntity.endDate,
        createdAt = scheduleEntity.createdAt,
        updatedAt = scheduleEntity.updatedAt,
        deletedAt = scheduleEntity.deletedAt,
      )

    fun fromRow(scheduleRow: ScheduleRow): Schedule =
      Schedule(
        id = scheduleRow.scheduleId,
        goalId = scheduleRow.goalId,
        userId = scheduleRow.userId,
        title = scheduleRow.title,
        description = scheduleRow.description,
        status = scheduleRow.toScheduleStatus(),
        startDate = scheduleRow.startDateToInstant(),
        endDate = scheduleRow.endDateToInstant(),
        createdAt = scheduleRow.createdAtToInstant(),
        updatedAt = scheduleRow.updatedAtToInstant(),
        deletedAt = scheduleRow.deletedAtToInstant(),
      )
  }
}
