package ai.aiminder.aiminderserver.auth.filter

import ai.aiminder.aiminderserver.auth.domain.AccessToken
import ai.aiminder.aiminderserver.auth.domain.RefreshToken
import ai.aiminder.aiminderserver.auth.property.CookieProperties
import ai.aiminder.aiminderserver.auth.service.TokenService
import ai.aiminder.aiminderserver.auth.service.UserService
import ai.aiminder.aiminderserver.common.util.logger
import kotlinx.coroutines.reactor.mono
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class CookieAuthenticationWebFilter(
  private val decoder: ReactiveJwtDecoder,
  private val tokenService: TokenService,
  private val userService: UserService,
  private val cookieProperties: CookieProperties,
) : WebFilter {
  private val logger = logger()

  private companion object {
    private const val ACCESS_COOKIE = "ACCESS_TOKEN"
    private const val REFRESH_COOKIE = "REFRESH_TOKEN"
  }

  override fun filter(
    exchange: ServerWebExchange,
    chain: WebFilterChain,
  ): Mono<Void> {
    val request: ServerHttpRequest = exchange.request
    val response: ServerHttpResponse = exchange.response

    val accessToken: AccessToken? = extractCookie(request, ACCESS_COOKIE)
    val refreshToken: RefreshToken? = extractCookie(request, REFRESH_COOKIE)

    if (accessToken.isNullOrBlank()) {
      return chain.filter(exchange)
    }

    return decoder
      .decode(accessToken)
      .flatMap { jwt ->
        val token: AccessToken = accessToken
        if (!tokenService.validateAccessToken(token)) {
          return@flatMap Mono.error(IllegalAccessException("Invalid access token"))
        }
        processAuthentication(jwt, chain, exchange)
      }.onErrorResume { err ->
        logger.error("Error during JWT accessToken validation: ${err.message}", err)
        if (refreshToken.isNullOrBlank()) {
          return@onErrorResume chain.filter(exchange)
        }
        decoder
          .decode(refreshToken)
          .flatMap {
            mono {
              val token: RefreshToken = refreshToken
              tokenService.validateRefreshToken(token)
              val user = userService.getUser(token)
              val newAccess = tokenService.createAccessToken(user)
              val newRefresh = tokenService.createRefreshToken(user)
              Pair(newAccess, newRefresh)
            }.flatMap { (newAccess, newRefresh) ->
              val accessTokenCookie = cookieProperties.buildCookie(ACCESS_COOKIE, newAccess)
              val refreshTokenCookie = cookieProperties.buildCookie(REFRESH_COOKIE, newRefresh)
              decoder.decode(newAccess).flatMap { newJwt ->
                response.addCookie(accessTokenCookie)
                response.addCookie(refreshTokenCookie)
                processAuthentication(newJwt, chain, exchange)
              }
            }
          }.onErrorResume { refreshErr ->
            logger.error("Error during refresh-token flow: ${refreshErr.message}", refreshErr)
            // Clear auth cookies and continue unauthenticated
            response.addCookie(expiredCookie(ACCESS_COOKIE))
            response.addCookie(expiredCookie(REFRESH_COOKIE))
            chain.filter(exchange)
          }
      }
  }

  private fun processAuthentication(
    jwt: Jwt,
    chain: WebFilterChain,
    exchange: ServerWebExchange,
  ): Mono<Void> {
    val authentication = JwtAuthenticationToken(jwt)
    val securityContext = SecurityContextImpl(authentication)
    return chain
      .filter(exchange)
      .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
  }

  private fun extractCookie(
    request: ServerHttpRequest,
    name: String,
  ): String? {
    request.cookies
      .getFirst(name)
      ?.value
      ?.let { return it }
    val raw = request.headers.getFirst("Cookie") ?: request.headers.getFirst("COOKIE")
    if (raw.isNullOrBlank()) return null
    return raw
      .split(";")
      .map { it.trim() }
      .firstOrNull { it.startsWith("$name=") }
      ?.substringAfter('=')
  }

  private fun expiredCookie(name: String): ResponseCookie =
    ResponseCookie
      .from(name, "")
      .let { if (cookieProperties.domain.isNotBlank()) it.domain(cookieProperties.domain) else it }
      .sameSite(cookieProperties.sameSite)
      .httpOnly(cookieProperties.httpOnly)
      .secure(cookieProperties.secure)
      .path("/")
      .maxAge(0)
      .build()
}