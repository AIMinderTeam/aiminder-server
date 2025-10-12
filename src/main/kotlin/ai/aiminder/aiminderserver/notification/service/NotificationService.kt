package ai.aiminder.aiminderserver.notification.service

import ai.aiminder.aiminderserver.notification.repository.NotificationRepository
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.stereotype.Service

@Service
class NotificationService(
  private val notificationRepository: NotificationRepository,
) {
  suspend fun getCountOfUncheckedNotifications(user: User): Int =
    notificationRepository.countByReceiverIdAndCheckedFalseAndDeletedAtIsNotNull(user.id)
}
