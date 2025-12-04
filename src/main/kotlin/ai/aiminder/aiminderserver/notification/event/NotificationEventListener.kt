package ai.aiminder.aiminderserver.notification.event

import ai.aiminder.aiminderserver.common.util.logger
import ai.aiminder.aiminderserver.notification.dto.CreateNotificationRequest
import ai.aiminder.aiminderserver.notification.service.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class NotificationEventListener(
  private val notificationService: NotificationService,
) {
  private val logger = logger()

  @EventListener
  fun consumeCreateDomainEvent(event: CreateNotificationEvent) {
    CoroutineScope(Dispatchers.IO).launch {
      runCatching {
        val request: CreateNotificationRequest = CreateNotificationRequest.from(event)
        notificationService.create(request)
      }.onFailure {
        logger.error("알림 생성을 실패했습니다 [ event : $event ]", it)
      }
    }
  }
}
