package ai.aiminder.aiminderserver.user.repository

import ai.aiminder.aiminderserver.user.entity.UserNotificationSettingsEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserNotificationSettingsRepository : CoroutineCrudRepository<UserNotificationSettingsEntity, UUID> {
  suspend fun findByUserId(userId: UUID): UserNotificationSettingsEntity?
}
