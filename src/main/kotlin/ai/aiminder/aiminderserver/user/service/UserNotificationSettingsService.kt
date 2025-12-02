package ai.aiminder.aiminderserver.user.service

import ai.aiminder.aiminderserver.user.domain.UserNotificationSettings
import ai.aiminder.aiminderserver.user.entity.UserNotificationSettingsEntity
import ai.aiminder.aiminderserver.user.repository.UserNotificationSettingsRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

@Service
class UserNotificationSettingsService(
  private val userNotificationSettingsRepository: UserNotificationSettingsRepository,
) {
  suspend fun getNotificationSettings(userId: UUID): UserNotificationSettings {
    val entity =
      userNotificationSettingsRepository.findById(userId)
        ?: createDefaultSettings(userId)

    return UserNotificationSettings.from(entity)
  }

  suspend fun updateNotificationSettings(
    userId: UUID,
    aiFeedbackEnabled: Boolean,
    aiFeedbackNotificationTime: LocalTime,
  ): UserNotificationSettings {
    val existingEntity =
      userNotificationSettingsRepository.findById(userId)
        ?: createDefaultSettings(userId)

    val updatedEntity =
      existingEntity.copy(
        aiFeedbackEnabled = aiFeedbackEnabled,
        aiFeedbackNotificationTime = aiFeedbackNotificationTime,
        updatedAt = Instant.now(),
      )

    val savedEntity = userNotificationSettingsRepository.save(updatedEntity)
    return UserNotificationSettings.from(savedEntity)
  }

  suspend fun updateAiFeedbackEnabled(
    userId: UUID,
    aiFeedbackEnabled: Boolean,
  ): UserNotificationSettings {
    val existingEntity =
      userNotificationSettingsRepository.findById(userId)
        ?: createDefaultSettings(userId)

    val updatedEntity =
      existingEntity.copy(
        aiFeedbackEnabled = aiFeedbackEnabled,
        updatedAt = Instant.now(),
      )

    val savedEntity = userNotificationSettingsRepository.save(updatedEntity)
    return UserNotificationSettings.from(savedEntity)
  }

  suspend fun updateAiFeedbackNotificationTime(
    userId: UUID,
    aiFeedbackNotificationTime: LocalTime,
  ): UserNotificationSettings {
    val existingEntity =
      userNotificationSettingsRepository.findById(userId)
        ?: createDefaultSettings(userId)

    val updatedEntity =
      existingEntity.copy(
        aiFeedbackNotificationTime = aiFeedbackNotificationTime,
        updatedAt = Instant.now(),
      )

    val savedEntity = userNotificationSettingsRepository.save(updatedEntity)
    return UserNotificationSettings.from(savedEntity)
  }

  private suspend fun createDefaultSettings(userId: UUID): UserNotificationSettingsEntity {
    val defaultEntity =
      UserNotificationSettingsEntity(
        id = userId,
        aiFeedbackEnabled = true,
        aiFeedbackNotificationTime = LocalTime.of(9, 0),
      )

    return userNotificationSettingsRepository.save(defaultEntity)
  }
}
