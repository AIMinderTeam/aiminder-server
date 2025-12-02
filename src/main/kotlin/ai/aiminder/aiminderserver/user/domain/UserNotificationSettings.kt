package ai.aiminder.aiminderserver.user.domain

import ai.aiminder.aiminderserver.user.entity.UserNotificationSettingsEntity
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

data class UserNotificationSettings(
  val userId: UUID,
  val aiFeedbackEnabled: Boolean,
  val aiFeedbackNotificationTime: LocalTime,
  val createdAt: Instant,
  val updatedAt: Instant,
) {
  companion object {
    fun from(entity: UserNotificationSettingsEntity): UserNotificationSettings =
      UserNotificationSettings(
        userId = entity.id!!,
        aiFeedbackEnabled = entity.aiFeedbackEnabled,
        aiFeedbackNotificationTime = entity.aiFeedbackNotificationTime,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
      )
  }
}
