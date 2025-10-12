package ai.aiminder.aiminderserver.notification.service

import ai.aiminder.aiminderserver.notification.domain.Notification
import ai.aiminder.aiminderserver.notification.dto.GetNotificationsRequestDto
import ai.aiminder.aiminderserver.notification.repository.NotificationRepository
import ai.aiminder.aiminderserver.user.domain.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service

@Service
class NotificationService(
  private val notificationRepository: NotificationRepository,
) {
  suspend fun getCountOfUncheckedNotifications(user: User): Int =
    notificationRepository.countByReceiverIdAndCheckedFalseAndDeletedAtIsNull(user.id)

  suspend fun get(dto: GetNotificationsRequestDto): Page<Notification> {
    val notifications: Flow<Notification> =
      notificationRepository
        .findAllByReceiverIdAndDeletedAtIsNull(dto.userId, dto.pageable)
        .map { Notification.from(it) }

    val totalCount = notificationRepository.countByReceiverIdAndDeletedAtIsNull(dto.userId)

    return PageImpl(notifications.toList(), dto.pageable, totalCount)
  }
}
