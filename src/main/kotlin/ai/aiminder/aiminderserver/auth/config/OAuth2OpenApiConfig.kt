package ai.aiminder.aiminderserver.auth.config

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OAuth2OpenApiConfig {
  @Bean
  fun oAuth2OpenApiCustomizer(): OpenApiCustomizer =
    OpenApiCustomizer { openApi ->
      run {
        val responses =
          ApiResponses()
            .addApiResponse(
              "302",
              ApiResponse().description("Google 인증 페이지로 리다이렉트"),
            ).addApiResponse("500", ApiResponse().description("서버 오류"))

        val op =
          Operation()
            .operationId("oauth2LoginGoogle")
            .tags(listOf("OAuth2 Login"))
            .summary("Google OAuth2 로그인")
            .description(
              "Google OAuth2를 통한 소셜 로그인을 시작합니다. " +
                "이 엔드포인트는 Google 인증 페이지로 리다이렉트됩니다.",
            ).responses(responses)
            .security(listOf()) // permitAll

        val pathItem = PathItem().get(op)
        openApi.path("/oauth2/authorization/google", pathItem)
      }

      run {
        val responses =
          ApiResponses()
            .addApiResponse(
              "302",
              ApiResponse().description("Kakao 인증 페이지로 리다이렉트"),
            ).addApiResponse("500", ApiResponse().description("서버 오류"))

        val op =
          Operation()
            .operationId("oauth2LoginKakao")
            .tags(listOf("OAuth2 Login"))
            .summary("Kakao OAuth2 로그인")
            .description(
              "Kakao OAuth2를 통한 소셜 로그인을 시작합니다. " +
                "이 엔드포인트는 Kakao 인증 페이지로 리다이렉트됩니다.",
            ).responses(responses)
            .security(listOf()) // permitAll

        val pathItem = PathItem().get(op)
        openApi.path("/oauth2/authorization/kakao", pathItem)
      }

      run {
        val setCookieHeader =
          Header()
            .description(
              "로그인 성공 시 발급되는 토큰 쿠키. " +
                "`ACCESS_TOKEN`, `REFRESH_TOKEN`가 `Set-Cookie`로 설정됩니다.",
            ).schema(Schema<Any>().type("string"))

        val responses =
          ApiResponses()
            .addApiResponse(
              "302",
              ApiResponse()
                .description("로그인 성공 시 클라이언트로 리다이렉트")
                .addHeaderObject("Set-Cookie", setCookieHeader),
            ).addApiResponse("401", ApiResponse().description("인증 실패"))
            .addApiResponse("500", ApiResponse().description("서버 오류"))

        val op =
          Operation()
            .operationId("oauth2Callback")
            .tags(listOf("OAuth2 Login"))
            .summary("OAuth2 콜백")
            .description(
              "OAuth2 공급자에서 인증을 마친 후 호출되는 콜백 엔드포인트입니다. " +
                "로그인 성공 시 `ACCESS_TOKEN`, `REFRESH_TOKEN` 쿠키가 설정되며, 클라이언트의 `/login/success`로 리다이렉트됩니다.",
            ).responses(responses)
            .security(listOf()) // permitAll

        // 스펙 유효성 오류 해결: 경로 변수 `{provider}` 정의 추가
        val providerParam =
          Parameter()
            .name("provider")
            .description("OAuth2 공급자 이름 (예: google, kakao)")
            .required(true)
            .`in`("path")
            .schema(StringSchema())

        op.addParametersItem(providerParam)

        val pathItem = PathItem().get(op)
        openApi.path("/login/oauth2/code/{provider}", pathItem)
      }
    }
}