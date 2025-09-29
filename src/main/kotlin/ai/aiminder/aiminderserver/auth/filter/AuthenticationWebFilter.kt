package ai.aiminder.aiminderserver.auth.filter

import ai.aiminder.aiminderserver.auth.domain.AccessToken
import ai.aiminder.aiminderserver.auth.domain.RefreshToken
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.auth.service.CookieManager
import ai.aiminder.aiminderserver.auth.service.TokenExtractor
import ai.aiminder.aiminderserver.auth.service.TokenRefreshService
import ai.aiminder.aiminderserver.auth.service.TokenValidator
import ai.aiminder.aiminderserver.common.util.logger
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class AuthenticationWebFilter(
  private val cookieManager: CookieManager,
  private val tokenExtractor: TokenExtractor,
  private val tokenValidator: TokenValidator,
  private val tokenRefreshService: TokenRefreshService,
) : WebFilter {
  private companion object {
    private const val BEARER_PREFIX = "Bearer "
  }

  private val logger = logger()

  override fun filter(
    exchange: ServerWebExchange,
    chain: WebFilterChain,
  ): Mono<Void> {
    val request: ServerHttpRequest = exchange.request
    val response: ServerHttpResponse = exchange.response

    logger.debug("CookieAuth start method=${request.method.name()} path=${request.uri.path} id=${request.id}")

    val accessToken: AccessToken? = tokenExtractor.extractAccessToken(request) ?: extractBearerToken(request)
    val refreshToken: RefreshToken? = tokenExtractor.extractRefreshToken(request)

    logger.debug(
      "CookieAuth cookies present: access=${accessToken.isNullOrBlank().not()} " +
        "refresh=${refreshToken.isNullOrBlank().not()} id=${request.id}",
    )

    if (accessToken.isNullOrBlank()) {
      logger.debug("CookieAuth no access token; skipping auth id=${request.id}")
      return chain.filter(exchange)
    }

    return tokenValidator
      .validateAndProcess(accessToken, exchange, chain)
      .onErrorResume { err ->
        if (err is AuthError.InvalidAccessToken || err is JwtException) {
          logger.debug(
            "CookieAuth error on access validation id=${request.id} path=${request.uri.path}: ${err.message}",
            err,
          )
          if (refreshToken.isNullOrBlank()) {
            logger.debug("CookieAuth no refresh token; proceeding unauthenticated id=${request.id}")
            return@onErrorResume chain.filter(exchange)
          }
          logger.debug("CookieAuth attempting refresh flow id=${request.id}")
          tokenRefreshService
            .refreshTokens(refreshToken, exchange, chain)
            .onErrorResume {
              if (it is AuthError || it is JwtException) {
                logger.debug(
                  "CookieAuth error during refresh flow id=${request.id} path=${request.uri.path}: ${it.message}",
                  it,
                )
                cookieManager.clearTokenCookies(response)
              }
              chain.filter(exchange)
            }
        } else {
          logger.debug("CookieAuth error on access validation id=${request.id} path=${request.uri.path}", err)
          chain.filter(exchange)
        }
      }
  }

  private fun extractBearerToken(request: ServerHttpRequest): String? {
    val authorizationHeader = request.headers.getFirst("Authorization") ?: return null

    if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
      logger.debug("BearerAuth authorization header doesn't start with 'Bearer ' id=${request.id}")
      return null
    }

    val token = authorizationHeader.substring(BEARER_PREFIX.length).trim()
    if (token.isBlank()) {
      logger.debug("BearerAuth empty token after 'Bearer ' prefix id=${request.id}")
      return null
    }

    logger.debug("BearerAuth found bearer token id=${request.id}")
    return token
  }
}
