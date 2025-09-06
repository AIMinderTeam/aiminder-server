package ai.aiminder.aiminderserver.goal.dto

import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.goal.domain.GoalStatus
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.data.domain.Pageable
import java.util.UUID

data class GetGoalsRequest(
  val status: GoalStatus = GoalStatus.ACTIVE,
)

data class GetGoalsRequestDto(
  val status: GoalStatus,
  val userId: UUID,
  val pageable: Pageable,
) {
  companion object {
    fun from(
      getGoalsRequest: GetGoalsRequest,
      user: User,
      pageable: PageableRequest,
    ): GetGoalsRequestDto =
      GetGoalsRequestDto(
        status = getGoalsRequest.status,
        userId = user.id,
        pageable = pageable.toDomain(),
      )
  }
}
