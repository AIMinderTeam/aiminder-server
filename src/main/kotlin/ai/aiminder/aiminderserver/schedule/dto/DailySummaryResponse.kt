package ai.aiminder.aiminderserver.schedule.dto

import ai.aiminder.aiminderserver.goal.domain.GoalStatus
import ai.aiminder.aiminderserver.schedule.domain.ScheduleStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Schema(description = "일별 목표 및 일정 요약 응답")
data class DailySummaryResponse(
  @Schema(description = "조회 날짜", example = "2024-01-15")
  val date: LocalDate,
  @Schema(description = "해당 날짜의 목표 목록 (연관 일정 포함)")
  val goals: List<DailyGoalWithSchedules>,
  @Schema(description = "해당 날짜 전체 일정 개수", example = "5")
  val totalScheduleCount: Int,
  @Schema(description = "해당 날짜 완료된 일정 개수", example = "2")
  val completedScheduleCount: Int,
)

@Schema(description = "일별 목표 정보 (연관 일정 포함)")
data class DailyGoalWithSchedules(
  @Schema(description = "목표 고유 ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
  val id: UUID,
  @Schema(description = "목표 제목", example = "Kotlin 학습")
  val title: String,
  @Schema(description = "목표 설명", example = "Kotlin 프로그래밍 언어 마스터하기")
  val description: String?,
  @Schema(description = "목표 달성 예정일", example = "2024-02-01T00:00:00Z")
  val targetDate: Instant,
  @Schema(description = "목표 상태", example = "INPROGRESS")
  val status: GoalStatus,
  @Schema(description = "연관된 이미지 ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
  val imageId: UUID?,
  @Schema(description = "연관된 이미지 경로", example = "/uploads/images/goal-image.jpg")
  val imagePath: String?,
  @Schema(description = "해당 날짜의 일정 목록")
  val schedules: List<DailyScheduleResponse>,
  @Schema(description = "해당 날짜 목표의 일정 개수", example = "2")
  val dailyScheduleCount: Int,
  @Schema(description = "해당 날짜 목표의 완료된 일정 개수", example = "1")
  val completedScheduleCount: Int,
)

@Schema(description = "일별 일정 정보")
data class DailyScheduleResponse(
  @Schema(description = "일정 고유 ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
  val id: UUID,
  @Schema(description = "일정 제목", example = "Kotlin 코루틴 학습")
  val title: String,
  @Schema(description = "일정 설명", example = "코루틴 기초 개념 학습")
  val description: String?,
  @Schema(description = "시작 일시", example = "2024-01-15T09:00:00Z")
  val startDate: Instant,
  @Schema(description = "종료 일시", example = "2024-01-15T10:00:00Z")
  val endDate: Instant,
  @Schema(description = "일정 상태", example = "READY")
  val status: ScheduleStatus,
)
