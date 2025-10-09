package ai.aiminder.aiminderserver.auth.service

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

interface AuthenticationProcessor {
  fun processAuthentication(
    jwt: Jwt,
    exchange: ServerWebExchange,
    chain: WebFilterChain,
  ): Mono<Void>
}
