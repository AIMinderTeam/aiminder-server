package ai.aiminder.aiminderserver.notification.repository

import ai.aiminder.aiminderserver.notification.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NotificationRepository : CoroutineCrudRepository<NotificationEntity, UUID> {
  suspend fun countByReceiverIdAndCheckedFalseAndDeletedAtIsNull(receiverId: UUID): Int

  suspend fun findAllByReceiverIdAndDeletedAtIsNull(
    receiverId: UUID,
    pageable: Pageable,
  ): Flow<NotificationEntity>

  suspend fun countByReceiverIdAndDeletedAtIsNull(receiverId: UUID): Long

  suspend fun findAllByReceiverIdAndCheckedFalseAndDeletedAtIsNull(receiverId: UUID): Flow<NotificationEntity>

  suspend fun findByIdAndDeletedAtIsNull(id: UUID): NotificationEntity?
}
