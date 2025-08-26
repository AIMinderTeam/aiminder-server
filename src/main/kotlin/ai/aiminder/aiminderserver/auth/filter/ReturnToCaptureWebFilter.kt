package ai.aiminder.aiminderserver.auth.filter

import ai.aiminder.aiminderserver.auth.property.CookieProperties
import ai.aiminder.aiminderserver.auth.security.AllowedRedirectValidator
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class ReturnToCaptureWebFilter(
  private val validator: AllowedRedirectValidator,
  private val cookieProperties: CookieProperties,
) : WebFilter {
  companion object {
    private const val COOKIE_NAME = "OAUTH2_RETURN_TO"
    private const val MAX_AGE_SECONDS: Long = 180
  }

  override fun filter(
    exchange: ServerWebExchange,
    chain: WebFilterChain,
  ): Mono<Void> {
    val path = exchange.request.path.value()
    if (path.startsWith("/oauth2/authorization/")) {
      val returnTo = exchange.request.queryParams.getFirst("return_to")
      if (validator.isAllowed(returnTo)) {
        val builder =
          ResponseCookie.from(COOKIE_NAME, returnTo!!)
            .let { if (cookieProperties.domain.isNotBlank()) it.domain(cookieProperties.domain) else it }
            .httpOnly(true)
            .secure(cookieProperties.secure)
            .sameSite(cookieProperties.sameSite)
            .path("/")
            .maxAge(MAX_AGE_SECONDS)

        exchange.response.addCookie(builder.build())
      }
    }
    return chain.filter(exchange)
  }
}