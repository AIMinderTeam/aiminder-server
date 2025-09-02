package ai.aiminder.aiminderserver.goal.domain

import java.time.Instant
import java.util.UUID

class Goal(
  val id: UUID,
  val userId: UUID,
  val title: String,
  val description: String,
  val targetDate: String,
  val isAiGenerated: String,
  val status: GoalStatus,
  val createdAt: Instant,
  val updatedAt: Instant,
  val deletedAt: Instant?,
)