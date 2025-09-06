package ai.aiminder.aiminderserver.goal.service

import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequestDto
import ai.aiminder.aiminderserver.goal.dto.GetGoalsRequestDto
import ai.aiminder.aiminderserver.goal.entity.GoalEntity
import ai.aiminder.aiminderserver.goal.repository.GoalRepository
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service

@Service
class GoalService(
  private val repository: GoalRepository,
) {
  suspend fun create(dto: CreateGoalRequestDto): Goal =
    GoalEntity
      .from(dto)
      .let { repository.save(it) }
      .let { Goal.from(it) }

  suspend fun get(dto: GetGoalsRequestDto): Page<Goal> =
    repository
      .findByStatusIsAndDeletedAtIsNullAndUserIdIs(
        status = dto.status,
        userId = dto.userId,
        pageable = dto.pageable,
      ).map { Goal.from(it) }
}
