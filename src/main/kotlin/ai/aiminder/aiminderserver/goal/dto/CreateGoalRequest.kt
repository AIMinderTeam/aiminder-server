package ai.aiminder.aiminderserver.goal.dto

import ai.aiminder.aiminderserver.assistant.domain.GoalDraft
import ai.aiminder.aiminderserver.assistant.domain.ServiceToolContext
import java.time.Instant
import java.util.UUID

data class CreateGoalRequest(
  val title: String,
  val description: String,
  val targetDate: Instant,
  val imageId: UUID? = null,
)

data class CreateGoalRequestDto(
  val userId: UUID,
  val title: String,
  val description: String,
  val targetDate: Instant,
  val imageId: UUID? = null,
  val isAiGenerated: Boolean = false,
) {
  companion object {
    fun from(
      draft: GoalDraft,
      context: ServiceToolContext,
    ): CreateGoalRequestDto =
      CreateGoalRequestDto(
        userId = context.userId,
        title = draft.goalTitle,
        description = draft.description,
        targetDate = draft.goalTargetDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
        isAiGenerated = true,
      )
  }
}
