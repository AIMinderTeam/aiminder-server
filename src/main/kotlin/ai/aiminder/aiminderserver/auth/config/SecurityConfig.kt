package ai.aiminder.aiminderserver.auth.config

import ai.aiminder.aiminderserver.auth.error.AuthErrorCode
import ai.aiminder.aiminderserver.auth.filter.JwtAuthenticationWebFilter
import ai.aiminder.aiminderserver.auth.property.OAuthProperties
import ai.aiminder.aiminderserver.auth.property.SecurityProperties
import ai.aiminder.aiminderserver.auth.service.AuthService
import ai.aiminder.aiminderserver.auth.service.JwtTokenService
import ai.aiminder.aiminderserver.auth.service.UserService
import ai.aiminder.aiminderserver.common.error.ErrorResponse
import ai.aiminder.aiminderserver.common.util.logger
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.server.ServerWebExchange

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
  private val oauthService: AuthService,
  private val oauthProperties: OAuthProperties,
  private val securityProperties: SecurityProperties,
  private val objectMapper: ObjectMapper,
) {
  private val logger = logger()

  @Bean
  fun securityWebFilterChain(
    http: ServerHttpSecurity,
    jwtTokenService: JwtTokenService,
    userService: UserService,
  ): SecurityWebFilterChain =
    http
      .cors { cors -> cors.configurationSource(corsConfigurationSource()) }
      .csrf { it.disable() }
      .addFilterBefore(
        jwtAuthenticationWebFilter(
          jwtTokenService,
          userService,
        ),
        SecurityWebFiltersOrder.AUTHENTICATION,
      ).authorizeExchange { exchanges ->
        exchanges
          .pathMatchers(*securityProperties.permitPaths.toTypedArray())
          .permitAll()
          .anyExchange()
          .authenticated()
      }.oauth2Login { oauth2 ->
        oauth2.authenticationSuccessHandler(authenticationSuccessHandler())
      }.exceptionHandling { exceptions ->
        exceptions.authenticationEntryPoint(unauthorizedEntryPoint())
      }.build()

  @Bean
  fun corsConfigurationSource(): CorsConfigurationSource {
    val configuration =
      CorsConfiguration().apply {
        allowedOriginPatterns = securityProperties.allowOriginPatterns
        allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        allowedHeaders = listOf("*")
        allowCredentials = true
        maxAge = 3600L
      }

    return UrlBasedCorsConfigurationSource().apply {
      registerCorsConfiguration("/**", configuration)
    }
  }

  @Bean
  fun jwtAuthenticationWebFilter(
    jwtTokenService: JwtTokenService,
    userService: UserService,
  ): JwtAuthenticationWebFilter = JwtAuthenticationWebFilter(jwtTokenService, userService)

  @Bean
  fun unauthorizedEntryPoint(): ServerAuthenticationEntryPoint =
    ServerAuthenticationEntryPoint { exchange: ServerWebExchange, exception: AuthenticationException ->
      val response: ServerHttpResponse = exchange.response
      response.statusCode = HttpStatus.UNAUTHORIZED
      response.headers.contentType = MediaType.APPLICATION_JSON

      val errorResponse = ErrorResponse<Unit>(HttpStatus.UNAUTHORIZED, AuthErrorCode.UNAUTHORIZED)
      // TODO errorResponse 적용
      mapOf(
        "error" to "AUTHENTICATION_REQUIRED",
        "message" to "인증이 필요합니다. 로그인을 진행해주세요.",
        "status" to HttpStatus.UNAUTHORIZED.value(),
        "timestamp" to System.currentTimeMillis(),
        "path" to exchange.request.path.value(),
      )

      val responseBody = objectMapper.writeValueAsString(errorResponse)
      val buffer: DataBuffer = response.bufferFactory().wrap(responseBody.toByteArray())
      response.writeWith(mono { buffer })
    }

  @Bean
  fun authenticationSuccessHandler(): ServerAuthenticationSuccessHandler =
    ServerAuthenticationSuccessHandler { webFilterExchange: WebFilterExchange, authentication: Authentication ->
      mono {
        val request = webFilterExchange.exchange.request
        val response = webFilterExchange.exchange.response

        try {
          val token = oauthService.processOAuth2User(authentication, request.path)
          response.headers.location = oauthProperties.getSuccessUri()
          // TODO refreshToken 적용
          // TODO errorResponse 적용
          response.statusCode = HttpStatus.OK
        } catch (error: Exception) {
          logger.error("Authentication success handler error: ${error.message}", error)
          val response = webFilterExchange.exchange.response
          response.statusCode = HttpStatus.UNAUTHORIZED
        }
      }.then(webFilterExchange.exchange.response.setComplete())
    }
}