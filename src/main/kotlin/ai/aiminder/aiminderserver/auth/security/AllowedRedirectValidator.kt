package ai.aiminder.aiminderserver.auth.security

import ai.aiminder.aiminderserver.auth.property.SecurityProperties
import org.springframework.stereotype.Component
import java.net.URI

@Component
class AllowedRedirectValidator(
  private val securityProperties: SecurityProperties,
) {
  fun isAllowed(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val parsed = runCatching { URI.create(url) }.getOrNull() ?: return false
    val scheme = parsed.scheme?.lowercase()
    val host = parsed.host?.lowercase()
    if (scheme !in setOf("http", "https")) return false
    if (host.isNullOrBlank()) return false
    val allowedHosts = securityProperties.allowedRedirectHosts.map { it.lowercase() }.toSet()
    return host in allowedHosts
  }
}