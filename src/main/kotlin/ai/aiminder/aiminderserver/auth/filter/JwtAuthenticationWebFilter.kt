package ai.aiminder.aiminderserver.auth.filter

import ai.aiminder.aiminderserver.auth.entity.UserEntity
import ai.aiminder.aiminderserver.auth.property.CookieProperties
import ai.aiminder.aiminderserver.auth.service.TokenService
import ai.aiminder.aiminderserver.auth.service.UserService
import ai.aiminder.aiminderserver.common.util.logger
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class JwtAuthenticationWebFilter(
  private val tokenService: TokenService,
  private val userService: UserService,
  private val cookieProperties: CookieProperties,
) : WebFilter {
  private val logger = logger()

  override fun filter(
    exchange: ServerWebExchange,
    chain: WebFilterChain,
  ): Mono<Void> {
    val token: String? =
      extractTokenFromRequest(exchange)
        ?.takeIf { tokenService.validateAccessToken(it) }

    return if (token != null) {
      mono {
        try {
          val user: UserEntity? = userService.getUser(token)
          if (user != null) {
            val authentication: Authentication =
              UsernamePasswordAuthenticationToken(
                user,
                null,
                emptyList(),
              )
            authentication
          } else {
            null
          }
        } catch (e: Exception) {
          logger.debug("JWT token validation failed: ${e.message}")
          null
        }
      }.flatMap { authentication ->
        if (authentication != null) {
          chain
            .filter(exchange)
            .contextWrite { ReactiveSecurityContextHolder.withAuthentication(authentication) }
        } else {
          logger.debug("JWT token validation failed")
          chain.filter(exchange)
        }
      }
    } else {
      logger.debug("No JWT token found in request")
      chain.filter(exchange)
    }
  }

  private fun extractTokenFromRequest(request: ServerWebExchange): String? {
    val authHeader = request.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
    return if (authHeader != null && authHeader.startsWith("Bearer ")) {
      authHeader.substring(7)
    } else {
      null
    }
  }
}