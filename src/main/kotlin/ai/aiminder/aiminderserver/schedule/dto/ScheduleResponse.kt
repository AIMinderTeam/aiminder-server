package ai.aiminder.aiminderserver.schedule.dto

import ai.aiminder.aiminderserver.schedule.domain.Schedule
import ai.aiminder.aiminderserver.schedule.domain.ScheduleStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "스케줄 응답 데이터")
data class ScheduleResponse(
  @Schema(description = "스케줄 고유 ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
  val id: UUID,
  @Schema(description = "연관된 목표 ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
  val goalId: UUID,
  @Schema(description = "사용자 ID", example = "2f6a3a4c-1c3b-4bde-9d2a-6c2c8b6a1e7f")
  val userId: UUID,
  @Schema(description = "스케줄 제목", example = "Daily Kotlin Practice")
  val title: String,
  @Schema(description = "스케줄 설명", example = "Practice Kotlin for 1 hour daily")
  val description: String?,
  @Schema(description = "스케줄 상태", example = "READY")
  val status: ScheduleStatus,
  @Schema(description = "시작일시", example = "2024-01-01T09:00:00Z")
  val startDate: Instant,
  @Schema(description = "종료일시", example = "2024-01-01T10:00:00Z")
  val endDate: Instant,
  @Schema(description = "생성일시", example = "2024-01-01T00:00:00Z")
  val createdAt: Instant,
  @Schema(description = "수정일시", example = "2024-01-01T00:00:00Z")
  val updatedAt: Instant,
  @Schema(description = "삭제일시", example = "null")
  val deletedAt: Instant?,
) {
  companion object {
    fun from(schedule: Schedule): ScheduleResponse =
      ScheduleResponse(
        id = schedule.id,
        goalId = schedule.goalId,
        userId = schedule.userId,
        title = schedule.title,
        description = schedule.description,
        status = schedule.status,
        startDate = schedule.startDate,
        endDate = schedule.endDate,
        createdAt = schedule.createdAt,
        updatedAt = schedule.updatedAt,
        deletedAt = schedule.deletedAt,
      )
  }
}
