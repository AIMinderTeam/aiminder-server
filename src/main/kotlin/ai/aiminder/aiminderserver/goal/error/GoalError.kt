package ai.aiminder.aiminderserver.goal.error

import ai.aiminder.aiminderserver.common.error.ServiceError
import org.springframework.http.HttpStatus
import java.util.UUID

sealed class GoalError(
  override val status: HttpStatus,
  override val message: String,
) : ServiceError() {
  override val mainCode: String = "GOAL"

  class GoalNotFound(
    goalId: UUID,
  ) : GoalError(HttpStatus.NOT_FOUND, "해당 ID: $goalId 를 가진 목표를 찾을 수 없습니다")
}
