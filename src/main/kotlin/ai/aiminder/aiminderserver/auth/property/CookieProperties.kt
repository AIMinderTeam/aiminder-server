package ai.aiminder.aiminderserver.auth.property

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.ResponseCookie

@ConfigurationProperties(prefix = "aiminder.cookie")
data class CookieProperties(
  val domain: String,
  val sameSite: String,
  val httpOnly: Boolean,
  val secure: Boolean,
) {
  fun buildCookie(
    name: String,
    value: String,
  ): ResponseCookie =
    ResponseCookie
      .from(name, value)
      .let { if (domain.isNotBlank()) it.domain(domain) else it }
      .sameSite(sameSite)
      .httpOnly(httpOnly)
      .secure(secure)
      .path("/")
      .build()
}