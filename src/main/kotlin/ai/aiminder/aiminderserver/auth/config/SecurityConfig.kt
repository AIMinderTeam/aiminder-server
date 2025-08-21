package ai.aiminder.aiminderserver.auth.config

import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.auth.filter.CookieAuthenticationWebFilter
import ai.aiminder.aiminderserver.auth.handler.TokenLoginSuccessHandler
import ai.aiminder.aiminderserver.auth.property.SecurityProperties
import ai.aiminder.aiminderserver.common.error.Response
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
  private val securityProperties: SecurityProperties,
  private val objectMapper: ObjectMapper,
  private val tokenLoginSuccessHandler: TokenLoginSuccessHandler,
  private val cookieAuthenticationWebFilter: CookieAuthenticationWebFilter,
) {
  @Bean
  fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
    http
      .cors { cors -> cors.configurationSource(corsConfigurationSource()) }
      .csrf { it.disable() }
      .authorizeExchange { exchanges ->
        exchanges
          .pathMatchers(*securityProperties.permitPaths.toTypedArray())
          .permitAll()
          .anyExchange()
          .authenticated()
      }.oauth2Login { oauth2 ->
        oauth2
          .authenticationSuccessHandler(tokenLoginSuccessHandler)
      }.exceptionHandling { exceptions ->
        exceptions.authenticationEntryPoint(unauthorizedEntryPoint())
      }.addFilterAt(cookieAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
      .build()

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
  fun unauthorizedEntryPoint(): ServerAuthenticationEntryPoint =
    ServerAuthenticationEntryPoint { exchange: ServerWebExchange, exception: AuthenticationException ->
      val responseDto = Response.from<Unit>(AuthError.UNAUTHORIZED)
      writeResponse(exchange.response, responseDto)
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