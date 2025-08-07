package ai.aiminder.aiminderserver.auth.config

import ai.aiminder.aiminderserver.auth.dto.OAuth2Response
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.auth.filter.JwtAuthenticationWebFilter
import ai.aiminder.aiminderserver.auth.property.SecurityProperties
import ai.aiminder.aiminderserver.auth.service.AuthService
import ai.aiminder.aiminderserver.auth.service.JwtTokenService
import ai.aiminder.aiminderserver.auth.service.UserService
import ai.aiminder.aiminderserver.common.error.Response
import ai.aiminder.aiminderserver.common.util.logger
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
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
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
  private val oauthService: AuthService,
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
        oauth2
          .authenticationSuccessHandler(authenticationSuccessHandler())
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
      val responseDto = Response.from<Unit>(AuthError.UNAUTHORIZED)
      writeResponse(exchange.response, responseDto)
    }

  @Bean
  fun authenticationSuccessHandler(): ServerAuthenticationSuccessHandler =
    ServerAuthenticationSuccessHandler { webFilterExchange: WebFilterExchange, authentication: Authentication ->
      mono {
        val request: ServerHttpRequest = webFilterExchange.exchange.request
        val response: ServerHttpResponse = webFilterExchange.exchange.response

        try {
          val accessToken: String = oauthService.processOAuth2User(authentication, request.path)
          val responseDto = Response.from(OAuth2Response(accessToken))
          writeResponse(response, responseDto).subscribe()
        } catch (error: Exception) {
          logger.error("Authentication success handler error: ${error.message}", error)
          val responseDto = Response.from<Unit>(AuthError.UNAUTHORIZED)
          writeResponse(response, responseDto).subscribe()
        }
      }.then(webFilterExchange.exchange.response.setComplete())
    }

  private fun <T> writeResponse(
    response: ServerHttpResponse,
    responseDto: Response<T>,
  ): Mono<Void> {
    response.statusCode = HttpStatusCode.valueOf(responseDto.statusCode)
    response.headers.contentType = MediaType.APPLICATION_JSON
    val responseBody = objectMapper.writeValueAsString(responseDto)
    val buffer: DataBuffer = response.bufferFactory().wrap(responseBody.toByteArray())
    return response.writeWith(mono { buffer })
  }
}