package ai.aiminder.aiminderserver.goal.dto

import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.domain.GoalStatus
import java.time.Instant
import java.util.UUID

data class GoalResponse(
  val id: UUID,
  val userId: UUID,
  val title: String,
  val description: String?,
  val targetDate: Instant,
  val isAiGenerated: Boolean,
  val status: GoalStatus,
  val imagePath: String?,
  val createdAt: Instant,
  val updatedAt: Instant,
  val deletedAt: Instant?,
) {
  companion object {
    fun from(
      goal: Goal,
      imagePath: String? = null,
    ): GoalResponse =
      GoalResponse(
        id = goal.id,
        userId = goal.userId,
        title = goal.title,
        description = goal.description,
        targetDate = goal.targetDate,
        isAiGenerated = goal.isAiGenerated,
        status = goal.status,
        imagePath = imagePath,
        createdAt = goal.createdAt,
        updatedAt = goal.updatedAt,
        deletedAt = goal.deletedAt,
      )
  }
}
