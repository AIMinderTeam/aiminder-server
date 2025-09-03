package ai.aiminder.aiminderserver.auth.filter

import ai.aiminder.aiminderserver.auth.property.CookieProperties
import ai.aiminder.aiminderserver.auth.security.AllowedRedirectValidator
import org.slf4j.LoggerFactory
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
  private val logger = LoggerFactory.getLogger(this::class.java)

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
      logger.debug("OAuth2 authorization path accessed: {}", path)
      val returnTo = exchange.request.queryParams.getFirst("return_to")
      logger.debug("Query param return_to: {}", returnTo)
      if (validator.isAllowed(returnTo)) {
        logger.info("Valid return_to detected. Setting cookie: {}", COOKIE_NAME)
        val builder =
          ResponseCookie
            .from(COOKIE_NAME, returnTo!!)
            .let { if (cookieProperties.domain.isNotBlank()) it.domain(cookieProperties.domain) else it }
            .httpOnly(true)
            .secure(cookieProperties.secure)
            .sameSite(cookieProperties.sameSite)
            .path("/")
            .maxAge(MAX_AGE_SECONDS)

        exchange.response.addCookie(builder.build())
        logger.debug(
          "Cookie set: name={}, domain={}, secure={}, sameSite={}, maxAge={}",
          COOKIE_NAME,
          cookieProperties.domain.ifBlank { "<none>" },
          cookieProperties.secure,
          cookieProperties.sameSite,
          MAX_AGE_SECONDS,
        )
      } else {
        logger.warn("Blocked invalid return_to redirect: {}", returnTo)
      }
    }
    return chain.filter(exchange)
  }
}
