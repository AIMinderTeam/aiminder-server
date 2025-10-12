package ai.aiminder.aiminderserver.assistant.repository

import ai.aiminder.aiminderserver.assistant.entity.ChatEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ChatRepository : CoroutineCrudRepository<ChatEntity, Long> {
  suspend fun findAllByConversationIdOrderByIdDesc(
    conversationId: UUID,
    pageable: Pageable,
  ): Flow<ChatEntity>

  suspend fun countByConversationIdOrderByIdDesc(
    conversationId: UUID,
    pageable: Pageable,
  ): Long
}
