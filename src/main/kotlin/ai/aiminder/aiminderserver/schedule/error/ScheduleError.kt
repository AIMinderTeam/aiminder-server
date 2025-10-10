package ai.aiminder.aiminderserver.schedule.error

import ai.aiminder.aiminderserver.common.error.ServiceError
import org.springframework.http.HttpStatus
import java.util.UUID

sealed class ScheduleError(
  override val status: HttpStatus,
  override val message: String,
) : ServiceError() {
  override val mainCode: String = "SCHEDULE"

  class NotFound : ScheduleError(
    HttpStatus.NOT_FOUND,
    "일정을 찾을 수 없습니다.",
  )

  class AccessDenied : ScheduleError(
    HttpStatus.FORBIDDEN,
    "해당 일정에 접근할 권한이 없습니다.",
  )

  class InvalidDateRange(id: UUID?) : ScheduleError(
    HttpStatus.BAD_REQUEST,
    "일정 ${id}의 시작일은 종료일보다 늦을 수 없습니다.",
  )
}
