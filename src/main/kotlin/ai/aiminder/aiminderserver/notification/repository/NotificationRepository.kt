package ai.aiminder.aiminderserver.notification.repository

import ai.aiminder.aiminderserver.notification.entity.NotificationEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NotificationRepository : CoroutineCrudRepository<NotificationEntity, UUID> {
  suspend fun countByReceiverIdAndCheckedFalseAndDeletedAtIsNull(receiverId: UUID): Int
}
