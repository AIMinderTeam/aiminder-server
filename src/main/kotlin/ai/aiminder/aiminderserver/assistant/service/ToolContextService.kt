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
    goalId: UUID?,
  ): Map<String, UUID> =
    mutableMapOf(
      CONVERSATION_ID to conversationId,
      USER_ID to userId,
    ).apply { goalId?.also { put(GOAL_ID, it) } }

  fun getContext(toolContext: ToolContext): ServiceToolContext {
    val conversationId: UUID =
      toolContext.context[CONVERSATION_ID] as? UUID
        ?: throw CommonError.InternalServerError("Conversation ID not found")

    val userId: UUID =
      toolContext.context[USER_ID] as? UUID
        ?: throw CommonError.InternalServerError("User ID not found")

    val goalId: UUID? =
      toolContext.context[GOAL_ID] as? UUID

    return ServiceToolContext(conversationId, userId, goalId)
  }

  companion object {
    const val CONVERSATION_ID = "CONVERSATION_ID"
    const val USER_ID = "USER_ID"
    const val GOAL_ID = "GOAL_ID"
  }
}
