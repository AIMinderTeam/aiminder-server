package ai.aiminder.aiminderserver.goal.repository

import ai.aiminder.aiminderserver.goal.domain.GoalStatus
import ai.aiminder.aiminderserver.goal.entity.GoalEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Query
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

  suspend fun findAllByUserIdAndStatusIsNotAndDeletedAtIsNull(
    userId: UUID,
    status: GoalStatus = GoalStatus.COMPLETED,
  ): Flow<GoalEntity>

  suspend fun countByStatusIsAndDeletedAtIsNullAndUserIdIs(
    status: GoalStatus,
    userId: UUID,
  ): Long

  @Query(
    """
      SELECT g.*
      FROM goals g, conversations c 
      WHERE g.goal_id = c.goal_id AND c.conversation_id = :conversationId 
      AND g.deleted_at IS NULL AND c.deleted_at IS NULL
    """,
  )
  suspend fun findByConversationId(conversationId: UUID): GoalEntity?
}
