package ai.aiminder.aiminderserver.auth.error

import ai.aiminder.aiminderserver.common.error.ServiceError

enum class AuthErrorCode(
  override val subCode: String,
  override val message: String,
) : ServiceError {
  UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다. 로그인을 진행해주세요."), ;

  override val mainCode: String = "AUTH"
}