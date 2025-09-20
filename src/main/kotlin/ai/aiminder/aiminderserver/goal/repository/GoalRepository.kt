package ai.aiminder.aiminderserver.goal.repository

import ai.aiminder.aiminderserver.goal.domain.GoalStatus
import ai.aiminder.aiminderserver.goal.entity.GoalEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GoalRepository : CoroutineCrudRepository<GoalEntity, UUID> {
  suspend fun findByStatusAndDeletedAtIsNullAndUserId(
    status: GoalStatus,
    userId: UUID,
    pageable: Pageable,
  ): Flow<GoalEntity>

  suspend fun countByStatusIsAndDeletedAtIsNullAndUserIdIs(
    status: GoalStatus,
    userId: UUID,
  ): Long
}
