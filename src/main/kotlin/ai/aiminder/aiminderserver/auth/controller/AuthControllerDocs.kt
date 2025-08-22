package ai.aiminder.aiminderserver.auth.controller

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * AuthController(/api/auth/user)에 대한 Swagger 문서 정의.
 * - 현재 로그인한 사용자 정보를 조회하는 엔드포인트를 문서화합니다.
 * - 성공 시 Response<GetUserResponse> 형태로 응답하며, 인증 실패 시 401을 반환합니다.
 */
@Configuration
class AuthControllerDocs {
  @Bean
  fun authControllerOpenApiCustomizer(): OpenApiCustomizer =
    OpenApiCustomizer { openApi ->
      // 응답 데이터 스키마 정의: GetUserResponse(id: UUID)
      val getUserSchema: Schema<Any> =
        Schema<Any>()
          .type("object")
          .addProperties(
            "id",
            Schema<Any>()
              .type("string")
              .format("uuid")
              .description("사용자 고유 식별자(UUID)"),
          )

      // 공통 래퍼(Response<T>) 스키마 정의 및 예시 구성
      val okContent =
        Content().addMediaType(
          org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
          MediaType().schema(
            Schema<Any>()
              .type("object")
              .addProperties(
                "statusCode",
                Schema<Any>().type("integer").format("int32").description("HTTP 상태 코드"),
              ).addProperties(
                "message",
                Schema<Any>().type("string").nullable(true).description("메시지(없을 수 있음)"),
              ).addProperties(
                "errorCode",
                Schema<Any>().type("string").nullable(true).description("에러 코드(없을 수 있음)"),
              ).addProperties(
                "data",
                getUserSchema,
              ),
          ).example(
            mapOf(
              "statusCode" to 200,
              "message" to null,
              "errorCode" to null,
              "data" to mapOf(
                "id" to "2f6a3a4c-1c3b-4bde-9d2a-6c2c8b6a1e7f",
              ),
            ),
          ),
        )

      val unauthorizedContent =
        Content().addMediaType(
          org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
          MediaType().schema(
            Schema<Any>()
              .type("object")
              .addProperties("statusCode", Schema<Any>().type("integer").format("int32"))
              .addProperties(
                "message",
                Schema<Any>().type("string").description("에러 메시지"),
              ).addProperties(
                "errorCode",
                Schema<Any>().type("string").description("에러 코드"),
              ).addProperties("data", Schema<Any>().nullable(true)),
          ).example(
            mapOf(
              "statusCode" to 401,
              "message" to "인증이 필요합니다. 로그인을 진행해주세요.",
              "errorCode" to "AUTH.UNAUTHORIZED",
              "data" to null,
            ),
          ),
        )

      val responses =
        ApiResponses()
          .addApiResponse(
            "200",
            ApiResponse()
              .description("성공: 현재 로그인한 사용자 정보 반환")
              .content(okContent),
          ).addApiResponse(
            "401",
            ApiResponse()
              .description("인증 실패: 토큰이 없거나 유효하지 않음")
              .content(unauthorizedContent),
          ).addApiResponse("500", ApiResponse().description("서버 오류"))

      val operation =
        Operation()
          .operationId("getCurrentUser")
          .tags(listOf("Auth"))
          .summary("현재 로그인한 사용자 조회")
          .description(
            "현재 인증된 사용자의 정보를 반환합니다. " +
              "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증을 사용합니다. " +
              "인증 정보가 없거나 유효하지 않으면 401이 반환됩니다.",
          ).responses(responses)

      val pathItem = PathItem().get(operation)
      openApi.path("/api/auth/user", pathItem)
    }
}
