package ai.aiminder.aiminderserver.user.controller

import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.dto.GetUserNotificationSettingsResponse
import ai.aiminder.aiminderserver.user.dto.UpdateAiFeedbackEnabledRequest
import ai.aiminder.aiminderserver.user.dto.UpdateAiFeedbackNotificationTimeRequest
import ai.aiminder.aiminderserver.user.service.UserNotificationSettingsService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user/notification-settings")
class UserNotificationSettingsController(
  private val userNotificationSettingsService: UserNotificationSettingsService,
) {
  @GetMapping
  suspend fun getNotificationSettings(
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<GetUserNotificationSettingsResponse> {
    val settings = userNotificationSettingsService.getNotificationSettings(user.id)
    val response = GetUserNotificationSettingsResponse.from(settings)
    return ServiceResponse.from(response)
  }

  @PatchMapping("/ai-feedback-enabled")
  suspend fun updateAiFeedbackEnabled(
    @RequestBody
    request: UpdateAiFeedbackEnabledRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<GetUserNotificationSettingsResponse> {
    val updatedSettings =
      userNotificationSettingsService.updateAiFeedbackEnabled(
        userId = user.id,
        aiFeedbackEnabled = request.aiFeedbackEnabled,
      )
    val response = GetUserNotificationSettingsResponse.from(updatedSettings)
    return ServiceResponse.from(response)
  }

  @PatchMapping("/ai-feedback-notification-time")
  suspend fun updateAiFeedbackNotificationTime(
    @RequestBody
    request: UpdateAiFeedbackNotificationTimeRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<GetUserNotificationSettingsResponse> {
    val updatedSettings =
      userNotificationSettingsService.updateAiFeedbackNotificationTime(
        userId = user.id,
        aiFeedbackNotificationTime = request.aiFeedbackNotificationTime,
      )
    val response = GetUserNotificationSettingsResponse.from(updatedSettings)
    return ServiceResponse.from(response)
  }
}
