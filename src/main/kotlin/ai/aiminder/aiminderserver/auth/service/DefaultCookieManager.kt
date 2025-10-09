package ai.aiminder.aiminderserver.auth.service

import ai.aiminder.aiminderserver.auth.domain.AccessToken
import ai.aiminder.aiminderserver.auth.domain.RefreshToken
import ai.aiminder.aiminderserver.auth.property.CookieProperties
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Service

@Service
class DefaultCookieManager(
  private val cookieProperties: CookieProperties,
) : CookieManager {
  private companion object {
    private const val ACCESS_COOKIE = "ACCESS_TOKEN"
    private const val REFRESH_COOKIE = "REFRESH_TOKEN"
  }

  override fun setTokenCookies(
    response: ServerHttpResponse,
    accessToken: AccessToken,
    refreshToken: RefreshToken,
  ) {
    val accessTokenCookie = cookieProperties.buildCookie(ACCESS_COOKIE, accessToken)
    val refreshTokenCookie = cookieProperties.buildCookie(REFRESH_COOKIE, refreshToken)
    response.addCookie(accessTokenCookie)
    response.addCookie(refreshTokenCookie)
  }

  override fun clearTokenCookies(response: ServerHttpResponse) {
    response.addCookie(createExpiredCookie(ACCESS_COOKIE))
    response.addCookie(createExpiredCookie(REFRESH_COOKIE))
  }

  override fun createExpiredCookie(name: String): ResponseCookie =
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
