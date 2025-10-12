package ai.aiminder.aiminderserver.notification.controller

import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.notification.service.NotificationService
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
  private val notificationService: NotificationService,
) {
  @GetMapping("/count")
  suspend fun getCountOfUncheckedNotifications(
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<Int> {
    val count: Int = notificationService.getCountOfUncheckedNotifications(user)
    return ServiceResponse.from(count)
  }
}
