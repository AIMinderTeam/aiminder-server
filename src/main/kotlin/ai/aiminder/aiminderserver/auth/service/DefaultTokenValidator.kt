package ai.aiminder.aiminderserver.auth.service

import ai.aiminder.aiminderserver.auth.domain.AccessToken
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.common.util.logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Service
class DefaultTokenValidator(
  @Qualifier("accessJwtDecoder") private val accessDecoder: ReactiveJwtDecoder,
  private val tokenService: TokenService,
  private val authenticationProcessor: AuthenticationProcessor,
) : TokenValidator {
  private val logger = logger()

  override fun validateAndProcess(
    accessToken: AccessToken,
    exchange: ServerWebExchange,
    chain: WebFilterChain,
  ): Mono<Void> {
    val request = exchange.request

    return accessDecoder
      .decode(accessToken)
      .flatMap { jwt ->
        logger.debug("TokenValidator access decoded: sub={} exp={} id={}", jwt.subject, jwt.expiresAt, request.id)
        val token: AccessToken = accessToken
        if (!tokenService.validateAccessToken(token)) {
          logger.debug("TokenValidator access token invalid for sub=${jwt.subject} id=${request.id}")
          return@flatMap Mono.error(AuthError.InvalidAccessToken())
        }
        logger.debug("TokenValidator access token valid for sub=${jwt.subject} id=${request.id}")
        authenticationProcessor.processAuthentication(jwt, exchange, chain)
      }
  }
}
