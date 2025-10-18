package ai.aiminder.aiminderserver.assistant.error

import ai.aiminder.aiminderserver.common.error.ServiceError
import org.springframework.http.HttpStatus
import java.util.UUID

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
    conversationId: UUID? = null,
    goalId: UUID? = null,
  ) : AssistantError(
      status = HttpStatus.NOT_FOUND,
      message = "대화방을 찾을 수 없습니다. conversationId: $conversationId, goalId: $goalId",
    )

  class ChatTransformError(
    chat: String,
  ) : AssistantError(
      status = HttpStatus.INTERNAL_SERVER_ERROR,
      message = "채팅 변환을 실패했습니다. chat: $chat",
    )
}
