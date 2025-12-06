package ai.aiminder.aiminderserver.user.repository

import ai.aiminder.aiminderserver.user.entity.UserNotificationSettingsEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalTime
import java.util.UUID

@Repository
interface UserNotificationSettingsRepository : CoroutineCrudRepository<UserNotificationSettingsEntity, UUID> {
  suspend fun findByUserId(userId: UUID): UserNotificationSettingsEntity?

  suspend fun findAllByAiFeedbackEnabledAndAiFeedbackNotificationTime(
    aiFeedbackEnabled: Boolean,
    aiFeedbackNotificationTime: LocalTime,
  ): Flow<UserNotificationSettingsEntity>
}
