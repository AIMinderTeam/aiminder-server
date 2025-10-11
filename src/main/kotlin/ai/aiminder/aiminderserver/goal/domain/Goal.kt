package ai.aiminder.aiminderserver.goal.domain

import ai.aiminder.aiminderserver.goal.dto.DeleteGoalRequestDto
import ai.aiminder.aiminderserver.goal.dto.UpdateGoalRequestDto
import ai.aiminder.aiminderserver.goal.entity.GoalEntity
import ai.aiminder.aiminderserver.goal.error.GoalError
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
  val imageId: UUID?,
  val createdAt: Instant,
  val updatedAt: Instant,
  val deletedAt: Instant?,
) {
  fun update(dto: UpdateGoalRequestDto): Goal {
    if (userId != dto.userId) throw GoalError.AccessDenied()
    return copy(
      title = dto.title ?: title,
      description = dto.description ?: description,
      targetDate = dto.targetDate ?: targetDate,
      imageId = dto.imageId ?: imageId,
      status = dto.status ?: status,
      updatedAt = Instant.now(),
    )
  }

  fun delete(dto: DeleteGoalRequestDto): Goal {
    if (userId != dto.userId) throw GoalError.AccessDenied()
    return copy(
      updatedAt = Instant.now(),
      deletedAt = Instant.now(),
    )
  }

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
        imageId = goalEntity.imageId,
        createdAt = goalEntity.createdAt,
        updatedAt = goalEntity.updatedAt,
        deletedAt = goalEntity.deletedAt,
      )
  }
}
