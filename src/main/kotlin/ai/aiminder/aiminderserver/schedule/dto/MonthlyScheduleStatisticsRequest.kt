package ai.aiminder.aiminderserver.schedule.dto

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.web.bind.annotation.RequestParam

data class MonthlyScheduleStatisticsRequest(
  @Parameter(
    description = "조회 연도",
    example = "2025",
    required = true,
  )
  @RequestParam
  val year: Int,
  @Parameter(
    description = "조회 월 (1-12)",
    example = "12",
    required = true,
  )
  @RequestParam
  val month: Int,
)
