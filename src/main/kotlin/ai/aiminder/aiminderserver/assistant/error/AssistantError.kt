package ai.aiminder.aiminderserver.assistant.error

import ai.aiminder.aiminderserver.common.error.ServiceError
import org.springframework.http.HttpStatus

sealed class AssistantError(
  override val status: HttpStatus,
  override val message: String,
) : ServiceError() {
  override val mainCode: String = "ASSISTANT"

  class InferenceError(
    message: String,
  ) : AssistantError(
      status = HttpStatus.INTERNAL_SERVER_ERROR,
      message = message,
    )
}
