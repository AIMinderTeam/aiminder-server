package ai.aiminder.aiminderserver.common.error

data class Response<T>(
  val statusCode: Int,
  val message: String? = null,
  val errorCode: String? = null,
  val data: T? = null,
) {
  companion object {
    fun <T> from(
      errorCode: ServiceError,
      data: T? = null,
    ): Response<T> =
      Response(
        statusCode = errorCode.statusCode.value(),
        errorCode = errorCode.toCode(),
        message = errorCode.message,
        data = data,
      )

    fun <T> from(
      data: T? = null,
      message: String? = null,
    ): Response<T> =
      Response(
        statusCode = 200,
        message = message,
        data = data,
      )
  }
}
