package ai.aiminder.aiminderserver.user.dto

import ai.aiminder.aiminderserver.user.domain.UserNotificationSettings
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalTime

data class GetUserNotificationSettingsResponse(
  val aiFeedbackEnabled: Boolean,
  @JsonFormat(pattern = "HH:mm")
  val aiFeedbackNotificationTime: LocalTime,
) {
  companion object {
    fun from(settings: UserNotificationSettings): GetUserNotificationSettingsResponse =
      GetUserNotificationSettingsResponse(
        aiFeedbackEnabled = settings.aiFeedbackEnabled,
        aiFeedbackNotificationTime = settings.aiFeedbackNotificationTime,
      )
  }
}
