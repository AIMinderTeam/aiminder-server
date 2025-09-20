package ai.aiminder.aiminderserver.auth.handler

import ai.aiminder.aiminderserver.auth.domain.TokenGroup
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.auth.property.CookieProperties
import ai.aiminder.aiminderserver.auth.property.SecurityProperties
import ai.aiminder.aiminderserver.auth.security.AllowedRedirectValidator
import ai.aiminder.aiminderserver.auth.service.AuthService
import ai.aiminder.aiminderserver.common.property.ClientProperties
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.common.util.logger
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.mono
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.DefaultServerRedirectStrategy
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.URI

@Component
class TokenLoginSuccessHandler(
  private val authService: AuthService,
  private val objectMapper: ObjectMapper,
  private val cookieProperties: CookieProperties,
  private val clientProperties: ClientProperties,
  private val securityProperties: SecurityProperties,
  private val allowedRedirectValidator: AllowedRedirectValidator,
) : ServerAuthenticationSuccessHandler {
  private val logger = logger()
  private val redirect = DefaultServerRedirectStrategy()

  override fun onAuthenticationSuccess(
    webFilterExchange: WebFilterExchange,
    authentication: Authentication,
  ): Mono<Void> =
    mono {
      val exchange: ServerWebExchange = webFilterExchange.exchange
      val request: ServerHttpRequest = exchange.request
      val response: ServerHttpResponse = exchange.response
      runCatching {
        val tokenGroup: TokenGroup = authService.processOAuth2User(authentication, request.path)
        response.addCookie(cookieProperties.buildCookie("ACCESS_TOKEN", tokenGroup.accessToken))
        response.addCookie(cookieProperties.buildCookie("REFRESH_TOKEN", tokenGroup.refreshToken))
      }.getOrElse {
        logger.error("Authentication success handler error: ${it.message}", it)
        val responseDto = ServiceResponse.from<Unit>(AuthError.Unauthorized())
        writeResponse(exchange.response, responseDto).subscribe()
      }
    }.then(
      redirect.sendRedirect(
        webFilterExchange.exchange,
        URI.create(resolveRedirectUrl(webFilterExchange.exchange)),
      ),
    )

  private fun resolveRedirectUrl(exchange: ServerWebExchange): String {
    val request = exchange.request
    val response = exchange.response

    val cookie = request.cookies.getFirst("OAUTH2_RETURN_TO")?.value
    val target = if (allowedRedirectValidator.isAllowed(cookie)) cookie else null

    if (cookie != null) {
      val deleteCookie =
        org.springframework.http.ResponseCookie
          .from("OAUTH2_RETURN_TO", "")
          .let { if (cookieProperties.domain.isNotBlank()) it.domain(cookieProperties.domain) else it }
          .httpOnly(true)
          .secure(cookieProperties.secure)
          .sameSite(cookieProperties.sameSite)
          .path("/")
          .maxAge(0)
          .build()
      response.addCookie(deleteCookie)
    }

    val base =
      securityProperties.defaultRedirectBaseUrl.ifBlank {
        clientProperties.url
      }
    return target ?: base.trimEnd('/')
  }

  private fun <T> writeResponse(
    response: ServerHttpResponse,
    responseDto: ServiceResponse<T>,
  ): Mono<Void> {
    response.statusCode = HttpStatusCode.valueOf(responseDto.statusCode)
    response.headers.contentType = MediaType.APPLICATION_JSON
    val responseBody = objectMapper.writeValueAsString(responseDto)
    val buffer: DataBuffer = response.bufferFactory().wrap(responseBody.toByteArray())
    return response.writeWith(mono { buffer })
  }
}
