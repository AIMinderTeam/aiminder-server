package ai.aiminder.aiminderserver.auth.service

import ai.aiminder.aiminderserver.auth.domain.AccessToken
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

interface TokenValidator {
  fun validateAndProcess(
    accessToken: AccessToken,
    exchange: ServerWebExchange,
    chain: WebFilterChain,
  ): Mono<Void>
}
