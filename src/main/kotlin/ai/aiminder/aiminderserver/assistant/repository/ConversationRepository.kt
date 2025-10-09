package ai.aiminder.aiminderserver.assistant.repository

import ai.aiminder.aiminderserver.assistant.entity.ConversationEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ConversationRepository : CoroutineCrudRepository<ConversationEntity, UUID>
