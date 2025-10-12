package ai.aiminder.aiminderserver.notification.entity

import ai.aiminder.aiminderserver.notification.domain.NotificationType
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("notifications")
data class NotificationEntity(
  @Id
  @Column("notification_id")
  @get:JvmName("notificationId")
  val id: UUID? = null,
  val type: NotificationType,
  val title: String,
  val description: String,
  val metadata: Map<String, String>,
  val receiverId: UUID,
  val checked: Boolean = false,
  val createdAt: Instant = Instant.now(),
  val updatedAt: Instant = createdAt,
  val deletedAt: Instant? = null,
) : Persistable<UUID> {
  override fun getId(): UUID? = id

  override fun isNew(): Boolean = id == null
}
