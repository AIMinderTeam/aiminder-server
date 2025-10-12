package ai.aiminder.aiminderserver.assistant.repository

import ai.aiminder.aiminderserver.assistant.entity.ChatEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatRepository : CoroutineCrudRepository<ChatEntity, Long>
