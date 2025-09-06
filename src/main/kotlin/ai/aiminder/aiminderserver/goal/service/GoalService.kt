package ai.aiminder.aiminderserver.goal.service

import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequestDto
import ai.aiminder.aiminderserver.goal.dto.GetGoalsRequestDto
import ai.aiminder.aiminderserver.goal.entity.GoalEntity
import ai.aiminder.aiminderserver.goal.repository.GoalRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
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

  suspend fun get(dto: GetGoalsRequestDto): Page<Goal> {
    val goals =
      repository
        .findByStatusAndDeletedAtIsNullAndUserId(
          status = dto.status,
          userId = dto.userId,
        ).map { Goal.from(it) }

    val totalCount =
      repository.countByStatusIsAndDeletedAtIsNullAndUserIdIs(
        status = dto.status,
        userId = dto.userId,
      )

    return PageImpl(goals.toList(), dto.pageable, totalCount)
  }
}
