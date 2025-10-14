package ai.aiminder.aiminderserver.notification.error

import ai.aiminder.aiminderserver.common.error.ServiceError
import org.springframework.http.HttpStatus
import java.util.UUID

sealed class NotificationError(
  override val status: HttpStatus,
  override val message: String,
) : ServiceError() {
  override val mainCode: String = "NOTIFICATION"

  class NotFound(
    notificationId: UUID,
  ) : NotificationError(HttpStatus.NOT_FOUND, "알림 ID: $notificationId 를 찾을 수 없습니다.")
}
