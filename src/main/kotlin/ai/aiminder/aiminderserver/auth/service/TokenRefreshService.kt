package ai.aiminder.aiminderserver.auth.service

import ai.aiminder.aiminderserver.auth.domain.RefreshToken
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

interface TokenRefreshService {
  fun refreshTokens(
    refreshToken: RefreshToken,
    exchange: ServerWebExchange,
    chain: WebFilterChain,
  ): Mono<Void>
}
