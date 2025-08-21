package ai.aiminder.aiminderserver.auth.handler

import ai.aiminder.aiminderserver.auth.domain.TokenGroup
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.auth.property.CookieProperties
import ai.aiminder.aiminderserver.auth.service.AuthService
import ai.aiminder.aiminderserver.common.error.Response
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
) : ServerAuthenticationSuccessHandler {
  private val logger = logger()
  private val redirect = DefaultServerRedirectStrategy()

  override fun onAuthenticationSuccess(
    webFilterExchange: WebFilterExchange,
    authentication: Authentication,
  ): Mono<Void> =
    mono {
      val exchange: ServerWebExchange = webFilterExchange.exchange ?: throw IllegalAccessException("")
      val request: ServerHttpRequest = exchange.request
      val response: ServerHttpResponse = exchange.response
      runCatching {
        val tokenGroup: TokenGroup = authService.processOAuth2User(authentication, request.path)
        response.addCookie(cookieProperties.buildCookie("ACCESS_TOKEN", tokenGroup.accessToken))
        response.addCookie(cookieProperties.buildCookie("REFRESH_TOKEN", tokenGroup.refreshToken))
      }.getOrElse {
        logger.error("Authentication success handler error: ${it.message}", it)
        val responseDto = Response.from<Unit>(AuthError.UNAUTHORIZED)
        writeResponse(exchange.response, responseDto)
      }
    }.then(redirect.sendRedirect(webFilterExchange.exchange, URI.create("/")))

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