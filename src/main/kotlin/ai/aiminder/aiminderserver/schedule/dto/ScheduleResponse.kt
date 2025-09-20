package ai.aiminder.aiminderserver.schedule.dto

import ai.aiminder.aiminderserver.schedule.domain.Schedule
import ai.aiminder.aiminderserver.schedule.domain.ScheduleStatus
import java.time.Instant
import java.util.UUID

data class ScheduleResponse(
  val id: UUID,
  val goalId: UUID,
  val userId: UUID,
  val title: String,
  val content: String?,
  val status: ScheduleStatus,
  val startDate: Instant,
  val endDate: Instant,
  val createdAt: Instant,
  val updatedAt: Instant,
  val deletedAt: Instant?,
) {
  companion object {
    fun from(schedule: Schedule): ScheduleResponse =
      ScheduleResponse(
        id = schedule.id,
        goalId = schedule.goalId,
        userId = schedule.userId,
        title = schedule.title,
        content = schedule.content,
        status = schedule.status,
        startDate = schedule.startDate,
        endDate = schedule.endDate,
        createdAt = schedule.createdAt,
        updatedAt = schedule.updatedAt,
        deletedAt = schedule.deletedAt,
      )
  }
}
