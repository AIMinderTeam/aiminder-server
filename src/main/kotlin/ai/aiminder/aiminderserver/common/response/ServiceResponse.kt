package ai.aiminder.aiminderserver.common.response

import ai.aiminder.aiminderserver.common.error.ServiceError
import org.springframework.data.domain.Page

data class ServiceResponse<T>(
  val statusCode: Int,
  val message: String? = null,
  val errorCode: String? = null,
  val data: T? = null,
  val pageable: PageableResponse? = null,
) {
  companion object {
    fun <T> from(
      serviceError: ServiceError,
      data: T? = null,
    ): ServiceResponse<T> =
      ServiceResponse(
        statusCode = serviceError.status.value(),
        errorCode = serviceError.code,
        message = serviceError.message,
        data = data,
      )

    fun <T> from(
      data: T? = null,
      message: String? = null,
    ): ServiceResponse<T> =
      ServiceResponse(
        statusCode = 200,
        message = message,
        data = data,
      )

    fun <T : Page<R>, R> from(
      data: T,
      message: String? = null,
    ): ServiceResponse<List<R>> =
      ServiceResponse(
        statusCode = 200,
        message = message,
        data = data.content,
        pageable = PageableResponse.from(data),
      )
  }
}
