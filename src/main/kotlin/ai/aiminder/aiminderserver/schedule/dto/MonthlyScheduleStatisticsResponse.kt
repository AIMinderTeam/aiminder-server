package ai.aiminder.aiminderserver.schedule.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "월별 일정 통계 응답 데이터")
data class MonthlyScheduleStatisticsResponse(
  @Schema(description = "조회 연도", example = "2025")
  val year: Int,
  @Schema(description = "조회 월", example = "12")
  val month: Int,
  @Schema(description = "일별 통계 목록")
  val dailyStatistics: List<DailyScheduleStatistics>,
) {
  companion object {
    fun from(
      year: Int,
      month: Int,
      dailyStatistics: List<DailyScheduleStatistics>,
    ): MonthlyScheduleStatisticsResponse =
      MonthlyScheduleStatisticsResponse(
        year = year,
        month = month,
        dailyStatistics = dailyStatistics,
      )
  }
}
