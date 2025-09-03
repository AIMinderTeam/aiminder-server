package ai.aiminder.aiminderserver.common.property

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aiminder.client")
data class ClientProperties(
  val url: String,
)
