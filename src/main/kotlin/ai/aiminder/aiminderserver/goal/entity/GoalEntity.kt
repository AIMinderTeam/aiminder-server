package ai.aiminder.aiminderserver.goal.entity

import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.domain.GoalStatus
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequestDto
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("goals")
data class GoalEntity(
  @Id
  @Column("goal_id")
  @get:JvmName("goalId")
  val id: UUID? = null,
  val userId: UUID,
  val title: String,
  val targetDate: Instant,
  val description: String? = null,
  val isAiGenerated: Boolean = false,
  val status: GoalStatus = GoalStatus.READY,
  val imageId: UUID? = null,
  val createdAt: Instant = Instant.now(),
  val updatedAt: Instant = createdAt,
  val deletedAt: Instant? = null,
) : Persistable<UUID> {
  override fun getId(): UUID? = id

  override fun isNew(): Boolean = id == null

  companion object {
    fun from(goal: Goal): GoalEntity =
      GoalEntity(
        id = goal.id,
        userId = goal.userId,
        title = goal.title,
        targetDate = goal.targetDate,
        description = goal.description,
        isAiGenerated = goal.isAiGenerated,
        status = goal.status,
        imageId = goal.imageId,
        createdAt = goal.createdAt,
        updatedAt = goal.updatedAt,
        deletedAt = goal.deletedAt,
      )

    fun from(dto: CreateGoalRequestDto): GoalEntity =
      GoalEntity(
        userId = dto.userId,
        title = dto.title,
        description = dto.description,
        targetDate = dto.targetDate,
        imageId = dto.imageId,
      )
  }
}
