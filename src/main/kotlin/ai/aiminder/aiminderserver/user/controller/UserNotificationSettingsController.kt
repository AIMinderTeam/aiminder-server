package ai.aiminder.aiminderserver.user.controller

import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.dto.GetUserNotificationSettingsResponse
import ai.aiminder.aiminderserver.user.dto.UpdateUserNotificationSettingsRequest
import ai.aiminder.aiminderserver.user.service.UserNotificationSettingsService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
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

  @PutMapping
  suspend fun updateNotificationSettings(
    @RequestBody
    request: UpdateUserNotificationSettingsRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<GetUserNotificationSettingsResponse> {
    val updatedSettings =
      userNotificationSettingsService.updateNotificationSettings(
        userId = user.id,
        aiFeedbackEnabled = request.aiFeedbackEnabled,
        aiFeedbackNotificationTime = request.aiFeedbackNotificationTime,
      )
    val response = GetUserNotificationSettingsResponse.from(updatedSettings)
    return ServiceResponse.from(response)
  }
}
