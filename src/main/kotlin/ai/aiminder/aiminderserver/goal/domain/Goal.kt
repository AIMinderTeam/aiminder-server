package ai.aiminder.aiminderserver.goal.domain

import ai.aiminder.aiminderserver.goal.entity.GoalEntity
import java.time.Instant
import java.util.UUID

data class Goal(
  val id: UUID,
  val userId: UUID,
  val title: String,
  val description: String?,
  val targetDate: Instant,
  val isAiGenerated: Boolean,
  val status: GoalStatus,
  val createdAt: Instant,
  val updatedAt: Instant,
  val deletedAt: Instant?,
) {
  companion object {
    fun from(goalEntity: GoalEntity): Goal =
      Goal(
        id = goalEntity.id!!,
        userId = goalEntity.userId,
        title = goalEntity.title,
        description = goalEntity.description,
        targetDate = goalEntity.targetDate,
        isAiGenerated = goalEntity.isAiGenerated,
        status = goalEntity.status,
        createdAt = goalEntity.createdAt,
        updatedAt = goalEntity.updatedAt,
        deletedAt = goalEntity.deletedAt,
      )
  }
}
