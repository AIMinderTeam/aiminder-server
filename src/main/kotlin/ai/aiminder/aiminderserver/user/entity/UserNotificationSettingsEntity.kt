package ai.aiminder.aiminderserver.user.entity

import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

@Table("user_notification_settings")
data class UserNotificationSettingsEntity(
  @Id
  @Column("notification_setting_id")
  @get:JvmName("notificationSettingId")
  val id: UUID? = null,
  val userId: UUID,
  val aiFeedbackEnabled: Boolean,
  val aiFeedbackNotificationTime: LocalTime,
  val createdAt: Instant = Instant.now(),
  val updatedAt: Instant = createdAt,
) : Persistable<UUID> {
  override fun getId(): UUID? = id

  override fun isNew(): Boolean = id == null
}
