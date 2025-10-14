package ai.aiminder.aiminderserver.notification.dto

import java.util.UUID

data class CheckNotificationRequestDto(
  val notificationId: UUID,
  val userId: UUID,
)
