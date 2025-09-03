package ai.aiminder.aiminderserver.auth.config

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LogoutOpenApiConfig {
  @Bean
  fun logoutOpenApiCustomizer(): OpenApiCustomizer =
    OpenApiCustomizer { openApi ->
      val responseSchema: Schema<Any> =
        Schema<Any>()
          .type("object")
          .addProperties("statusCode", Schema<Any>().type("integer").format("int32"))
          .addProperties("message", Schema<Any>().type("string"))
          .addProperties("errorCode", Schema<Any>().type("string").nullable(true))
          .addProperties("data", Schema<Any>().nullable(true))

      val okJson =
        Content().addMediaType(
          org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
          MediaType().schema(responseSchema).example(
            mapOf(
              "statusCode" to 200,
              "message" to "Logged out",
              "errorCode" to null,
              "data" to null,
            ),
          ),
        )

      val setCookieHeader =
        Header()
          .description(
            "브라우저 쿠키 삭제를 위한 Set-Cookie 헤더. " +
              "`ACCESS_TOKEN` 및 `REFRESH_TOKEN`가 `Max-Age=0; Path=/`로 설정됩니다.",
          ).schema(Schema<Any>().type("string"))

      val responses =
        ApiResponses()
          .addApiResponse(
            "200",
            ApiResponse()
              .description("성공: 쿠키 삭제 및 메시지 반환")
              .content(okJson)
              .addHeaderObject("Set-Cookie", setCookieHeader),
          ).addApiResponse("401", ApiResponse().description("인증 필요 또는 세션 없음"))

      val operation =
        Operation()
          .operationId("logout")
          .tags(listOf("Auth"))
          .summary("로그아웃")
          .description(
            "현재 사용자 세션의 리프레시 토큰을 무효화하고, " +
              "`ACCESS_TOKEN`, `REFRESH_TOKEN` 쿠키를 만료시킵니다.",
          ).responses(responses)

      val pathItem = PathItem().post(operation)

      openApi.path("/api/auth/logout", pathItem)
    }
}
