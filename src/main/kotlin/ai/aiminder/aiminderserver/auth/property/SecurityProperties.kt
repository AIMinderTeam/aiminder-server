package ai.aiminder.aiminderserver.auth.property

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aiminder.security")
data class SecurityProperties(
  val permitPaths: List<String>,
  val allowOriginPatterns: List<String>,
)