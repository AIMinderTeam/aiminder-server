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
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.util.context.Context

@Component
class CookieAuthenticationWebFilter(
  private val decoder: ReactiveJwtDecoder,
  private val tokenService: TokenService,
  private val userService: UserService,
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

    if (accessToken.isNullOrBlank()) {
      return chain.filter(exchange)
    }

    return decoder
      .decode(accessToken)
      .flatMap { jwt ->
        tokenService.validateAccessToken(accessToken)
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
              tokenService.validateRefreshToken(refreshToken)
              val user = userService.getUser(refreshToken)
              val newAccess = tokenService.createAccessToken(user)
              val newRefresh = tokenService.createRefreshToken(user)
              Pair(newAccess, newRefresh)
            }.flatMap { (newAccess, newRefresh) ->
              val accessTokenCookie = cookieProperties.buildCookie("ACCESS_TOKEN", newAccess)
              val refreshTokenCookie = cookieProperties.buildCookie("REFRESH_TOKEN", newRefresh)
              decoder.decode(newAccess).flatMap { newJwt ->
                response.addCookie(accessTokenCookie)
                response.addCookie(refreshTokenCookie)
                processAuthentication(newJwt, chain, exchange)
              }
            }
          }.onErrorResume {
            response.addCookie(
              ResponseCookie
                .from("ACCESS_TOKEN", "")
                .maxAge(0)
                .path("/")
                .build(),
            )
            response.addCookie(
              ResponseCookie
                .from("REFRESH_TOKEN", "")
                .maxAge(0)
                .path("/")
                .build(),
            )
            Mono.empty()
          }
      }
  }

  private fun processAuthentication(
    jwt: Jwt,
    chain: WebFilterChain,
    exchange: ServerWebExchange,
  ): Mono<Void> {
    val authentication = JwtAuthenticationToken(jwt, null)
    val context: Context = ReactiveSecurityContextHolder.withAuthentication(authentication)
    return chain
      .filter(exchange)
      .contextWrite(context)
  }
}