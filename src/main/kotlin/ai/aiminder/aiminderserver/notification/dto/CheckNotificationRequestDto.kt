package ai.aiminder.aiminderserver.notification.dto

import java.util.UUID

data class CheckNotificationRequestDto(
  val userId: UUID,
  val notificationId: UUID? = null,
)
