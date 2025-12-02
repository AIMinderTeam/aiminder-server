package ai.aiminder.aiminderserver.user.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalTime

data class UpdateAiFeedbackNotificationTimeRequest(
  @JsonFormat(pattern = "HH:mm")
  val aiFeedbackNotificationTime: LocalTime,
)
