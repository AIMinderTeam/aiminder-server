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
    goalId: UUID? = null,
    conversationId: UUID? = null,
  ) : GoalError(HttpStatus.NOT_FOUND, "해당 ID 를 가진 목표를 찾을 수 없습니다. goalId: $goalId, conversationId: $conversationId")

  class AccessDenied :
    GoalError(
      HttpStatus.FORBIDDEN,
      "해당 목표에 접근할 권한이 없습니다.",
    )
}
