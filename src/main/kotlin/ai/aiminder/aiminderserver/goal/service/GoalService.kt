package ai.aiminder.aiminderserver.goal.service

import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequest
import ai.aiminder.aiminderserver.goal.repository.GoalRepository
import org.springframework.stereotype.Service

@Service
class GoalService(
  private val repository: GoalRepository,
) {
  fun create(request: CreateGoalRequest): Goal {
  }
}