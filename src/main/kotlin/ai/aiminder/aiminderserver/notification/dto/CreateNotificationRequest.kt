package ai.aiminder.aiminderserver.notification.dto

import ai.aiminder.aiminderserver.notification.domain.NotificationType
import ai.aiminder.aiminderserver.notification.event.CreateNotificationEvent
import java.util.UUID

data class CreateNotificationRequest(
  val title: String,
  val content: String,
  val note: Map<String, String>,
  val receiverId: UUID,
  val type: NotificationType,
) {
  companion object {
    fun from(event: CreateNotificationEvent) =
      CreateNotificationRequest(
        title = event.title,
        content = event.content,
        note = event.createNote(),
        receiverId = event.receiverId,
        type = event.notificationType,
      )
  }
}
