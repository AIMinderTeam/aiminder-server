package ai.aiminder.aiminderserver.auth.service

import ai.aiminder.aiminderserver.common.util.logger
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Service

@Service
class DefaultTokenExtractor : TokenExtractor {
  private val logger = logger()

  private companion object {
    private const val ACCESS_COOKIE = "ACCESS_TOKEN"
    private const val REFRESH_COOKIE = "REFRESH_TOKEN"
  }

  override fun extractAccessToken(request: ServerHttpRequest): String? = extractCookie(request, ACCESS_COOKIE)

  override fun extractRefreshToken(request: ServerHttpRequest): String? = extractCookie(request, REFRESH_COOKIE)

  private fun extractCookie(
    request: ServerHttpRequest,
    name: String,
  ): String? {
    request.cookies
      .getFirst(name)
      ?.value
      ?.let {
        logger.debug("TokenExtractor extractCookie name=$name found=true (from cookies) id=${request.id}")
        return it
      }
    val raw = request.headers.getFirst("Cookie") ?: request.headers.getFirst("COOKIE")
    if (raw.isNullOrBlank()) return null
    val value =
      raw
        .split(";")
        .map { it.trim() }
        .firstOrNull { it.startsWith("$name=") }
        ?.substringAfter('=')
    logger.debug(
      "TokenExtractor extractCookie name=$name found=${!value.isNullOrBlank()} (from header) id=${request.id}",
    )
    return value
  }
}
