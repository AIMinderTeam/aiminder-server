package ai.aiminder.aiminderserver.schedule.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate
import java.util.UUID

@Schema(description = "일별 목표 및 일정 요약 조회 요청")
data class DailySummaryRequest(
  @Schema(description = "조회할 날짜 (ISO 8601 날짜 형식)", example = "2024-01-15")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  val date: LocalDate,
)

data class DailySummaryRequestDto(
  val userId: UUID,
  val date: LocalDate,
)
