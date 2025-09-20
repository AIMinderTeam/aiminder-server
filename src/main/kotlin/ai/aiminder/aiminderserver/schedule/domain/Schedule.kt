package ai.aiminder.aiminderserver.schedule.domain

import ai.aiminder.aiminderserver.schedule.entity.ScheduleEntity
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
    fun from(scheduleEntity: ScheduleEntity): Schedule =
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
  }
}
