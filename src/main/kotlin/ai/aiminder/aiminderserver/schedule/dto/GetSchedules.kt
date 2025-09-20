package ai.aiminder.aiminderserver.schedule.dto

import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.schedule.domain.ScheduleStatus
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.util.UUID

data class GetSchedulesRequest(
  val goalId: UUID? = null,
  val status: ScheduleStatus? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
)

data class GetSchedulesRequestDto(
  val userId: UUID,
  val goalId: UUID? = null,
  val status: ScheduleStatus? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val pageable: Pageable,
) {
  companion object {
    fun from(
      getSchedulesRequest: GetSchedulesRequest,
      user: User,
      pageable: PageableRequest,
    ): GetSchedulesRequestDto =
      GetSchedulesRequestDto(
        userId = user.id,
        goalId = getSchedulesRequest.goalId,
        status = getSchedulesRequest.status,
        startDate = getSchedulesRequest.startDate,
        endDate = getSchedulesRequest.endDate,
        pageable = pageable.toDomain(),
      )
  }
}
