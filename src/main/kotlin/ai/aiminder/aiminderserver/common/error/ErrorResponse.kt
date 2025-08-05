package ai.aiminder.aiminderserver.common.error

import org.springframework.http.HttpStatusCode

data class ErrorResponse<T>(
  val statusCode: HttpStatusCode,
  val errorCode: String,
  val message: String,
  val data: T? = null,
) {
  constructor(
    statusCode: HttpStatusCode,
    errorCode: ServiceError,
    data: T? = null,
  ) : this(statusCode = statusCode, errorCode = errorCode.toCode(), message = errorCode.message, data = data)
}