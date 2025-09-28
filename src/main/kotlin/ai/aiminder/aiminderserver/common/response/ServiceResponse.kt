package ai.aiminder.aiminderserver.common.response

import ai.aiminder.aiminderserver.common.error.ServiceError
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

@Schema(description = "공통 API 응답 형식")
data class ServiceResponse<T>(
  @Schema(description = "HTTP 상태 코드", example = "200")
  val statusCode: Int,
  @Schema(description = "응답 메시지", example = "성공")
  val message: String? = null,
  @Schema(description = "에러 코드", example = "null")
  val errorCode: String? = null,
  @Schema(description = "응답 데이터")
  val data: T? = null,
  @Schema(description = "페이지네이션 정보")
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
