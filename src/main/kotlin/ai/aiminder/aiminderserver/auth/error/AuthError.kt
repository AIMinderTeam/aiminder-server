package ai.aiminder.aiminderserver.auth.error

import ai.aiminder.aiminderserver.common.error.ServiceError
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

enum class AuthError(
  override val subCode: String,
  override val message: String,
  override val statusCode: HttpStatusCode,
) : ServiceError {
  UNAUTHORIZED(
    subCode = "UNAUTHORIZED",
    message = "인증이 필요합니다. 로그인을 진행해주세요.",
    statusCode = HttpStatus.UNAUTHORIZED,
  ), ;

  override val mainCode: String = "AUTH"
}
