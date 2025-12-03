package ai.aiminder.aiminderserver.notification.event

import ai.aiminder.aiminderserver.notification.domain.NotificationType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
sealed interface CreateNotificationEvent {
  val receiverId: UUID
  val notificationType: NotificationType
  val title: String
  val content: String

  fun createNote(): Map<String, String> =
    Json.decodeFromString(Json.encodeToString(kotlinx.serialization.serializer(), this))
}
