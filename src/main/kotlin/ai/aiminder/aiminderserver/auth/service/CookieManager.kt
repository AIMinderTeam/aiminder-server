package ai.aiminder.aiminderserver.auth.service

import ai.aiminder.aiminderserver.auth.domain.AccessToken
import ai.aiminder.aiminderserver.auth.domain.RefreshToken
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpResponse

interface CookieManager {
  fun setTokenCookies(
    response: ServerHttpResponse,
    accessToken: AccessToken,
    refreshToken: RefreshToken,
  )

  fun clearTokenCookies(response: ServerHttpResponse)

  fun createExpiredCookie(name: String): ResponseCookie
}
