package ai.aiminder.aiminderserver.notification.domain

import ai.aiminder.aiminderserver.notification.dto.CreateNotificationRequest
import ai.aiminder.aiminderserver.notification.entity.NotificationEntity
import java.time.Instant
import java.util.UUID

data class Notification(
  val id: UUID,
  val type: NotificationType,
  val title: String,
  val description: String,
  val metadata: Map<String, String>,
  val receiverId: UUID,
  val checked: Boolean,
  val createdAt: Instant,
  val updatedAt: Instant,
  val deletedAt: Instant?,
) {
  companion object {
    fun from(entity: NotificationEntity): Notification =
      Notification(
        id = entity.id!!,
        type = entity.type,
        title = entity.title,
        description = entity.description,
        metadata = entity.metadata,
        receiverId = entity.receiverId,
        checked = entity.checked,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        deletedAt = entity.deletedAt,
      )
  }
}
