package ai.aiminder.aiminderserver.notification.controller

import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.notification.domain.Notification
import ai.aiminder.aiminderserver.notification.dto.GetNotificationsRequestDto
import ai.aiminder.aiminderserver.notification.service.NotificationService
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
  private val notificationService: NotificationService,
) : NotificationControllerDocs {
  @GetMapping("/count")
  override suspend fun getCountOfUncheckedNotifications(
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<Int> {
    val count: Int = notificationService.getCountOfUncheckedNotifications(user)
    return ServiceResponse.from(count)
  }

  @GetMapping
  override suspend fun getNotifications(
    pageable: PageableRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<List<Notification>> {
    val dto = GetNotificationsRequestDto.from(user, pageable)
    val notifications: Page<Notification> = notificationService.get(dto)
    return ServiceResponse.from(notifications)
  }
}
