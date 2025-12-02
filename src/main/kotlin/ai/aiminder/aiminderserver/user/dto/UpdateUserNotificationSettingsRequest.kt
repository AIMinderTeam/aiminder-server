package ai.aiminder.aiminderserver.user.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalTime

data class UpdateUserNotificationSettingsRequest(
  val aiFeedbackEnabled: Boolean,
  @JsonFormat(pattern = "HH:mm")
  val aiFeedbackNotificationTime: LocalTime,
)
