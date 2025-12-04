package ai.aiminder.aiminderserver.schedule.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "일별 일정 통계 데이터")
data class DailyScheduleStatistics(
  @Schema(description = "해당 월의 일자 (1-31)", example = "1")
  val date: Int,
  @Schema(description = "전체 일정 개수", example = "5")
  val totalCount: Int,
  @Schema(description = "완료된 일정 개수", example = "3")
  val completedCount: Int,
  @Schema(description = "완료율 (0.0 ~ 1.0)", example = "0.6")
  val completionRate: Double,
) {
  companion object {
    fun from(
      date: Int,
      totalCount: Int,
      completedCount: Int,
    ): DailyScheduleStatistics {
      val completionRate = if (totalCount > 0) completedCount.toDouble() / totalCount else 0.0
      return DailyScheduleStatistics(
        date = date,
        totalCount = totalCount,
        completedCount = completedCount,
        completionRate = completionRate,
      )
    }
  }
}
