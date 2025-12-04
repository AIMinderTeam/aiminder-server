package ai.aiminder.aiminderserver.notification.event

import ai.aiminder.aiminderserver.notification.domain.NotificationType
import ai.aiminder.aiminderserver.notification.event.serializer.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CreateFeedbackEvent(
  val goalTitle: String,
  @Serializable(with = UUIDSerializer::class)
  val conversationId: UUID,
  @Serializable(with = UUIDSerializer::class)
  override val receiverId: UUID,
) : CreateNotificationEvent {
  override val notificationType: NotificationType = NotificationType.ASSISTANT_FEEDBACK
  override val title: String = "AI 비서 알림"
  override val content: String = "\"$goalTitle\" 목표에 대한 피드백을 확인하세요."
}
