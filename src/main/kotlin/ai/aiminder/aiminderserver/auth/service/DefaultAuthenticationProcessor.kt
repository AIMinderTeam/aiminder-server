package ai.aiminder.aiminderserver.auth.service

import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.common.util.logger
import ai.aiminder.aiminderserver.common.util.toUUID
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.service.UserService
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Service
class DefaultAuthenticationProcessor(
  private val userService: UserService,
) : AuthenticationProcessor {
  private val logger = logger()

  override fun processAuthentication(
    jwt: Jwt,
    exchange: ServerWebExchange,
    chain: WebFilterChain,
  ): Mono<Void> {
    val requestId = exchange.request.id
    val userId =
      runCatching { jwt.subject.toUUID() }
        .getOrElse {
          logger.error("AuthProcessor invalid subject on JWT; sub='${jwt.subject}' id=$requestId", it)
          return chain.filter(exchange)
        }

    return mono { User.from(userService.getUserById(userId)) }
      .flatMap { user ->
        val authorities = listOf(SimpleGrantedAuthority(Role.USER.name))
        val authentication = UsernamePasswordAuthenticationToken(user, null, authorities).apply { details = jwt }
        val securityContext = SecurityContextImpl(authentication)
        logger.debug(
          "AuthProcessor setting SecurityContext sub={} authorities={} id={}",
          jwt.subject,
          authentication.authorities,
          requestId,
        )
        chain
          .filter(exchange)
          .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
      }
  }
}
