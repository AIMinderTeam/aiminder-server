package ai.aiminder.aiminderserver.schedule.dto

import java.util.UUID

data class GoalScheduleStatistics(
  val goalId: UUID,
  val totalCount: Int,
  val completedCount: Int,
)
