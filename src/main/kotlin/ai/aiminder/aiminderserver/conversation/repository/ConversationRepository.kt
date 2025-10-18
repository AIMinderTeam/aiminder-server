package ai.aiminder.aiminderserver.conversation.repository

import ai.aiminder.aiminderserver.conversation.entity.ConversationEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ConversationRepository : CoroutineCrudRepository<ConversationEntity, UUID> {
  fun findByGoalId(goalId: UUID): ConversationEntity?
}
