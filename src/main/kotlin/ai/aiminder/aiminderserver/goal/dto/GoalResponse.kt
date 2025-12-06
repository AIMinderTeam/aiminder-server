package ai.aiminder.aiminderserver.goal.dto

import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.domain.GoalStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "목표 응답 데이터")
data class GoalResponse(
  @Schema(description = "목표 고유 ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
  val id: UUID,
  @Schema(description = "사용자 ID", example = "2f6a3a4c-1c3b-4bde-9d2a-6c2c8b6a1e7f")
  val userId: UUID,
  @Schema(description = "목표 제목", example = "Learn Kotlin")
  val title: String,
  @Schema(description = "목표 설명", example = "Master Kotlin programming language")
  val description: String?,
  @Schema(description = "목표 달성 예정일", example = "2024-12-31T23:59:59Z")
  val targetDate: Instant,
  @Schema(description = "AI 생성 여부", example = "true")
  val isAiGenerated: Boolean,
  @Schema(description = "목표 상태", example = "ACTIVE")
  val status: GoalStatus,
  @Schema(description = "연관된 이미지 경로", example = "/images/goal-image.jpg")
  val imagePath: String?,
  @Schema(description = "전체 일정 개수", example = "10")
  val totalScheduleCount: Int,
  @Schema(description = "완료된 일정 개수", example = "3")
  val completedScheduleCount: Int,
  @Schema(description = "생성일시", example = "2024-01-01T00:00:00Z")
  val createdAt: Instant,
  @Schema(description = "수정일시", example = "2024-01-01T00:00:00Z")
  val updatedAt: Instant,
  @Schema(description = "삭제일시", example = "null")
  val deletedAt: Instant?,
) {
  companion object {
    fun from(
      goal: Goal,
      imagePath: String? = null,
      totalScheduleCount: Int = 0,
      completedScheduleCount: Int = 0,
    ): GoalResponse =
      GoalResponse(
        id = goal.id,
        userId = goal.userId,
        title = goal.title,
        description = goal.description,
        targetDate = goal.targetDate,
        isAiGenerated = goal.isAiGenerated,
        status = goal.status,
        imagePath = imagePath,
        totalScheduleCount = totalScheduleCount,
        completedScheduleCount = completedScheduleCount,
        createdAt = goal.createdAt,
        updatedAt = goal.updatedAt,
        deletedAt = goal.deletedAt,
      )
  }
}
