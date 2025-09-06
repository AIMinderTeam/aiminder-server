package ai.aiminder.aiminderserver.goal.repository

import ai.aiminder.aiminderserver.goal.domain.GoalStatus
import ai.aiminder.aiminderserver.goal.entity.GoalEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GoalRepository : CoroutineCrudRepository<GoalEntity, UUID> {
  suspend fun findByStatusIsAndDeletedAtIsNullAndUserIdIs(
    status: GoalStatus,
    userId: UUID,
    pageable: Pageable,
  ): Page<GoalEntity>
}
