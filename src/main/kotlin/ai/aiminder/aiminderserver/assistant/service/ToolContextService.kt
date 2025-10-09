package ai.aiminder.aiminderserver.assistant.service

import ai.aiminder.aiminderserver.assistant.domain.ServiceToolContext
import ai.aiminder.aiminderserver.common.error.CommonError
import org.springframework.ai.chat.model.ToolContext
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ToolContextService {
  fun create(
    conversationId: UUID,
    userId: UUID,
  ): Map<String, UUID> =
    mapOf(
      CONVERSATION_ID to conversationId,
      USER_ID to userId,
    )

  fun getContext(toolContext: ToolContext): ServiceToolContext {
    val conversationId = (
      toolContext.context[CONVERSATION_ID] as? UUID
        ?: throw CommonError.InternalServerError("Conversation ID not found")
    )

    val userId = (
      toolContext.context[USER_ID] as? UUID
        ?: throw CommonError.InternalServerError("User ID not found")
    )

    return ServiceToolContext(conversationId, userId)
  }

  companion object {
    const val CONVERSATION_ID = "CONVERSATION_ID"
    const val USER_ID = "USER_ID"
  }
}
