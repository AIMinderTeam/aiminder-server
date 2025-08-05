package ai.aiminder.aiminderserver.auth.property

import io.jsonwebtoken.security.Keys
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.crypto.SecretKey

@ConfigurationProperties(prefix = "aiminder.jwt")
data class JwtProperties(
  private val secret: String,
  private val expiration: Long,
) {
  val secretKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

  fun addExpirationTime(now: Instant): Instant = now.plus(expiration, ChronoUnit.SECONDS)
}