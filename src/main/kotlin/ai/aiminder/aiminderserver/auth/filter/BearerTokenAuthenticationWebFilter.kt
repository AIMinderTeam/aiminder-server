package ai.aiminder.aiminderserver.auth.filter

import ai.aiminder.aiminderserver.auth.domain.AccessToken
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.auth.service.TokenService
import ai.aiminder.aiminderserver.common.util.logger
import ai.aiminder.aiminderserver.common.util.toUUID
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.service.UserService
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class BearerTokenAuthenticationWebFilter(
  @Qualifier("accessJwtDecoder") private val accessDecoder: ReactiveJwtDecoder,
  private val tokenService: TokenService,
  private val userService: UserService,
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

    return accessDecoder
      .decode(bearerToken)
      .flatMap { jwt ->
        logger.debug("BearerAuth access decoded: sub={} exp={} id={}", jwt.subject, jwt.expiresAt, request.id)
        val token: AccessToken = bearerToken
        if (!tokenService.validateAccessToken(token)) {
          logger.warn("BearerAuth access token invalid for sub=${jwt.subject} id=${request.id}")
          return@flatMap Mono.error(AuthError.InvalidAccessToken())
        }
        logger.debug("BearerAuth access token valid for sub=${jwt.subject} id=${request.id}")
        processAuthentication(jwt, chain, exchange)
      }.onErrorResume { err ->
        logger.error(
          "BearerAuth error on access validation id=${request.id} path=${request.uri.path}: ${err.message}",
          err,
        )
        chain.filter(exchange)
      }
  }

  private fun processAuthentication(
    jwt: Jwt,
    chain: WebFilterChain,
    exchange: ServerWebExchange,
  ): Mono<Void> {
    val requestId = exchange.request.id
    val userId =
      runCatching { jwt.subject.toUUID() }
        .getOrElse {
          logger.error("BearerAuth invalid subject on JWT; sub='${jwt.subject}' id=$requestId", it)
          return chain.filter(exchange)
        }

    return mono { User.from(userService.getUserById(userId)) }
      .flatMap { user ->
        val authorities = listOf(SimpleGrantedAuthority(Role.USER.name))
        val authentication = UsernamePasswordAuthenticationToken(user, null, authorities).apply { details = jwt }
        val securityContext = SecurityContextImpl(authentication)
        logger.debug(
          "BearerAuth setting SecurityContext sub={} authorities={} id={}",
          jwt.subject,
          authentication.authorities,
          requestId,
        )
        chain
          .filter(exchange)
          .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
      }.onErrorResume { err ->
        logger.error("BearerAuth failed to resolve user from JWT subject id=$requestId: ${err.message}", err)
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
