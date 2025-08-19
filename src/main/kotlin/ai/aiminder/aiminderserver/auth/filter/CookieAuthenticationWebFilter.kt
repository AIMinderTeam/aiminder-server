package ai.aiminder.aiminderserver.auth.filter

import ai.aiminder.aiminderserver.auth.domain.AccessToken
import ai.aiminder.aiminderserver.auth.domain.RefreshToken
import ai.aiminder.aiminderserver.auth.property.CookieProperties
import ai.aiminder.aiminderserver.auth.service.TokenService
import ai.aiminder.aiminderserver.common.util.logger
import kotlinx.coroutines.reactor.mono
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtTypeValidator.jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.util.context.Context
import kotlin.math.log

@Component
class CookieAuthenticationWebFilter(
  private val decoder: ReactiveJwtDecoder,
  private val tokenService: TokenService,
  private val cookieProperties: CookieProperties,
) : WebFilter {
  private val logger = logger()

  override fun filter(
    exchange: ServerWebExchange,
    chain: WebFilterChain,
  ): Mono<Void> {
    val request: ServerHttpRequest = exchange.request
    val response: ServerHttpResponse = exchange.response

    val accessToken: AccessToken? = request.cookies.getFirst("ACCESS_TOKEN")?.value
    val refreshToken: RefreshToken? = request.cookies.getFirst("REFRESH_TOKEN")?.value

    if (accessToken.isNullOrBlank().not()) {
      decoder
        .decode(accessToken)
        .flatMap { jwt ->
          tokenService.validateAccessToken(accessToken)
          if (jwt == null) {
            logger.error("Invalid JWT accessToken: $accessToken")
            return@flatMap Mono.empty<JwtAuthenticationToken>()
          }
          val authentication: JwtAuthenticationToken = JwtAuthenticationToken(jwt, null)
          val context: Context = ReactiveSecurityContextHolder.withAuthentication(authentication)
          chain
            .filter(exchange)
            .contextWrite(context)
        }.onErrorResume {
          logger.error("Error during JWT accessToken validation: ${it.message}", it)
          if (refreshToken.isNullOrBlank().not()) {
            decoder
              .decode(refreshToken)
              .flatMap { jwt ->
                mono {
                  tokenService.validateRefreshToken(refreshToken)
                  if (jwt == null) {
                    logger.error("Invalid JWT refreshToken: $refreshToken")
                    return@mono Mono.empty<JwtAuthenticationToken>()
                  }
                }
              }
          } else {
            logger.error("No JWT accessToken and no JWT refreshToken found in request", it)
            chain.filter(exchange)
          }
        }
    }

    return chain.filter(exchange)
  }
}