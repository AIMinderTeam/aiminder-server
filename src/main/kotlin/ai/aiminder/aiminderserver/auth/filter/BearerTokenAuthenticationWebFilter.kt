package ai.aiminder.aiminderserver.auth.filter

import ai.aiminder.aiminderserver.auth.domain.AccessToken
import ai.aiminder.aiminderserver.auth.service.TokenValidator
import ai.aiminder.aiminderserver.common.util.logger
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class BearerTokenAuthenticationWebFilter(
  private val tokenValidator: TokenValidator,
) : WebFilter {
  private val logger = logger()

  private companion object {
    private const val BEARER_PREFIX = "Bearer "
  }

  override fun filter(
    exchange: ServerWebExchange,
    chain: WebFilterChain,
  ): Mono<Void> {
    val request: ServerHttpRequest = exchange.request

    logger.debug("BearerAuth start method=${request.method.name()} path=${request.uri.path} id=${request.id}")

    val bearerToken: AccessToken? = extractBearerToken(request)

    if (bearerToken.isNullOrBlank()) {
      logger.debug("BearerAuth no bearer token; skipping auth id=${request.id}")
      return chain.filter(exchange)
    }

    return tokenValidator
      .validateAndProcess(bearerToken, exchange, chain)
      .onErrorResume { err ->
        logger.debug(
          "BearerAuth error on access validation id=${request.id} path=${request.uri.path}: ${err.message}",
          err,
        )
        chain.filter(exchange)
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
