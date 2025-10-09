package ai.aiminder.aiminderserver.auth.service

import ai.aiminder.aiminderserver.auth.domain.RefreshToken
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.auth.property.CookieProperties
import ai.aiminder.aiminderserver.common.util.logger
import ai.aiminder.aiminderserver.user.service.UserService
import kotlinx.coroutines.reactor.mono
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Service
class DefaultTokenRefreshService(
  @Qualifier("accessJwtDecoder") private val accessDecoder: ReactiveJwtDecoder,
  @Qualifier("refreshJwtDecoder") private val refreshDecoder: ReactiveJwtDecoder,
  private val tokenService: TokenService,
  private val userService: UserService,
  private val cookieProperties: CookieProperties,
  private val cookieManager: CookieManager,
  private val authenticationProcessor: AuthenticationProcessor,
) : TokenRefreshService {
  private val logger = logger()

  override fun refreshTokens(
    refreshToken: RefreshToken,
    exchange: ServerWebExchange,
    chain: WebFilterChain,
  ): Mono<Void> {
    val request = exchange.request
    val response = exchange.response

    logger.debug("TokenRefresh attempting refresh flow id=${request.id}")
    return refreshDecoder
      .decode(refreshToken)
      .flatMap {
        mono {
          val token: RefreshToken = refreshToken
          logger.debug("TokenRefresh decoding refresh token succeeded id=${request.id}")
          if (!tokenService.validateRefreshToken(token)) {
            logger.debug("TokenRefresh refresh token invalid id=${request.id}")
            throw AuthError.InvalidRefreshToken()
          }
          val user = userService.getUser(token)
          logger.debug("TokenRefresh refresh validated; issuing new tokens userId={} id={}", user.id, request.id)
          val newAccess = tokenService.createAccessToken(user)
          val newRefresh = tokenService.createRefreshToken(user)
          Pair(newAccess, newRefresh)
        }.flatMap { (newAccess, newRefresh) ->
          accessDecoder.decode(newAccess).flatMap { newJwt ->
            cookieManager.setTokenCookies(response, newAccess, newRefresh)
            logger.info("TokenRefresh reissued tokens; cookies set id=${request.id}")
            logger.debug(
              "TokenRefresh cookie flags domain=${cookieProperties.domain} sameSite=${cookieProperties.sameSite} " +
                "secure=${cookieProperties.secure} id=${request.id}",
            )
            authenticationProcessor.processAuthentication(newJwt, exchange, chain)
          }
        }
      }
  }
}
