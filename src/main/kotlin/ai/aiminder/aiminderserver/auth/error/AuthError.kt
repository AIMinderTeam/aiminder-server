package ai.aiminder.aiminderserver.auth.error

import ai.aiminder.aiminderserver.common.error.ServiceError
import org.springframework.http.HttpStatus

sealed class AuthError(
  override val status: HttpStatus,
  override val message: String,
) : ServiceError() {
  override val mainCode: String = "AUTH"

  class Unauthorized : AuthError(status = HttpStatus.UNAUTHORIZED, message = "인증이 필요합니다. 로그인을 진행해주세요.")
}
