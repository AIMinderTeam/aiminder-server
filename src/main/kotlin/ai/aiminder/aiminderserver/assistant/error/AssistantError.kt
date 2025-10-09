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

  class ConversationNotFound(
    conversationId: String,
  ) : AssistantError(
      status = HttpStatus.NOT_FOUND,
      message = "대화방을 찾을 수 없습니다. conversationId: $conversationId",
    )
}
