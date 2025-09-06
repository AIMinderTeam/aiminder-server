package ai.aiminder.aiminderserver.common.error

import org.springframework.http.HttpStatus

sealed class CommonError(
  override val status: HttpStatus,
  override val message: String,
) : ServiceError() {
  override val mainCode: String = "COMMON"

  class InvalidRequest(
    message: String?,
  ) : CommonError(HttpStatus.BAD_REQUEST, message ?: "잘못된 요청입니다.")

  class NoResourceFound : CommonError(HttpStatus.NOT_FOUND, "존재하지 않는 API 입니다.")

  class InvalidMethod(
    message: String?,
  ) : CommonError(HttpStatus.METHOD_NOT_ALLOWED, message ?: "유효하지 않은 HTTP 메서드 입니다.")

  class InvalidMediaType(
    message: String?,
  ) : CommonError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message ?: "유효하지 않은 MediaType 입니다.")

  class InternalServerError(
    message: String?,
  ) : CommonError(HttpStatus.INTERNAL_SERVER_ERROR, message ?: "알 수 없는 에러가 발생했습니다.")
}
