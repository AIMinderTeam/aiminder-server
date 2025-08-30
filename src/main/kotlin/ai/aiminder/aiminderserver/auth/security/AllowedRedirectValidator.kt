package ai.aiminder.aiminderserver.auth.security

import ai.aiminder.aiminderserver.auth.property.SecurityProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI

@Component
class AllowedRedirectValidator(
  private val securityProperties: SecurityProperties,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  fun isAllowed(url: String?): Boolean {
    logger.debug("Validating redirect URL: {}", url)
    if (url.isNullOrBlank()) {
      logger.warn("Rejecting redirect: URL is null or blank")
      return false
    }
    val parsed =
      runCatching { URI.create(url) }
        .onFailure { logger.warn("Rejecting redirect: failed to parse URL: {}", url) }
        .getOrNull() ?: return false
    val scheme = parsed.scheme?.lowercase()
    val host = parsed.host?.lowercase()
    logger.debug("Parsed redirect URL -> scheme={}, host={}", scheme, host)
    if (scheme !in setOf("http", "https")) {
      logger.warn("Rejecting redirect: disallowed scheme: {}", scheme)
      return false
    }
    if (host.isNullOrBlank()) {
      logger.warn("Rejecting redirect: host is missing")
      return false
    }
    val allowedHosts = securityProperties.allowedRedirectHosts.map { it.lowercase() }.toSet()
    logger.debug("Allowed redirect hosts: {}", allowedHosts)
    val allowed = host in allowedHosts
    if (allowed) {
      logger.info("Redirect host allowed: {}", host)
    } else {
      logger.warn("Redirect host NOT allowed: {}", host)
    }
    return allowed
  }
}