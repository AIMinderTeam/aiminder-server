package ai.aiminder.aiminderserver.auth.error

import ai.aiminder.aiminderserver.common.error.ServiceError
import org.springframework.http.HttpStatus

sealed class AuthError(
  override val status: HttpStatus,
  override val message: String,
) : ServiceError() {
  override val mainCode: String = "AUTH"

  class UnsupportedProvider(
    message: String,
  ) : AuthError(HttpStatus.BAD_REQUEST, message)

  class Unauthorized : AuthError(HttpStatus.UNAUTHORIZED, "인증이 필요합니다. 로그인을 진행해주세요.")

  class InvalidAccessToken : AuthError(HttpStatus.UNAUTHORIZED, "잘못된 액세스 토큰입니다.")

  class InvalidRefreshToken : AuthError(HttpStatus.UNAUTHORIZED, "잘못된 리프래시 토큰입니다.")

  class NotFoundOAuthId : AuthError(HttpStatus.INTERNAL_SERVER_ERROR, "OAuth2 식별자를 찾을 수 없습니다.")
}
