package ai.aiminder.aiminderserver.auth.handler

import ai.aiminder.aiminderserver.auth.entity.UserEntity
import ai.aiminder.aiminderserver.auth.property.CookieProperties
import ai.aiminder.aiminderserver.auth.repository.RefreshTokenRepository
import ai.aiminder.aiminderserver.common.util.logger
import kotlinx.coroutines.reactor.mono
import org.springframework.http.ResponseCookie
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class TokenLogoutHandler(
  private val refreshTokenRepository: RefreshTokenRepository,
  private val cookieProperties: CookieProperties,
) : ServerLogoutHandler {
  private val logger = logger()

  private companion object {
    private const val ACCESS_COOKIE = "ACCESS_TOKEN"
    private const val REFRESH_COOKIE = "REFRESH_TOKEN"
  }

  override fun logout(
    exchange: WebFilterExchange,
    authentication: Authentication,
  ): Mono<Void> {
    val response = exchange.exchange.response

    val deleteRefresh =
      mono {
        val principal = authentication.principal
        if (principal is UserEntity && principal.id != null) {
          runCatching { refreshTokenRepository.deleteByUserId(principal.id) }
            .onFailure {
              logger.warn(
                "Logout failed to delete refresh token for user=${principal.id}: ${it.message}",
                it,
              )
            }
        }
      }

    response.addCookie(expiredCookie(ACCESS_COOKIE))
    response.addCookie(expiredCookie(REFRESH_COOKIE))

    return deleteRefresh.then()
  }

  private fun expiredCookie(name: String): ResponseCookie =
    ResponseCookie
      .from(name, "")
      .let { if (cookieProperties.domain.isNotBlank()) it.domain(cookieProperties.domain) else it }
      .sameSite(cookieProperties.sameSite)
      .httpOnly(cookieProperties.httpOnly)
      .secure(cookieProperties.secure)
      .path("/")
      .maxAge(0)
      .build()
}