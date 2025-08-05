package ai.aiminder.aiminderserver.auth.service

import ai.aiminder.aiminderserver.auth.domain.User
import ai.aiminder.aiminderserver.auth.property.JwtProperties
import ai.aiminder.aiminderserver.common.util.logger
import ai.aiminder.aiminderserver.common.util.toUUID
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import java.util.UUID

@Service
class JwtTokenService(
  private val jwtProperties: JwtProperties,
) {
  private val key = jwtProperties.secretKey
  private val logger = logger()

  fun generateToken(user: User): String {
    val now = Instant.now()
    val expireDate = jwtProperties.addExpirationTime(now)

    return Jwts
      .builder()
      .subject(user.id.toString())
      .claim("provider", user.provider)
      .issuedAt(Date.from(now))
      .expiration(Date.from(expireDate))
      .signWith(key)
      .compact()
  }

  fun validateToken(token: String): Boolean =
    runCatching {
      Jwts
        .parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
      true
    }.getOrElse { exception ->
      logger.error("Invalid JWT token: ${exception.message}", exception)
      false
    }

  fun getUserIdFromToken(token: String): UUID =
    runCatching {
      Jwts
        .parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .payload
        .subject
        .toUUID()
    }.getOrElse {
      logger.error("Error getting user ID from token: ${it.message}", it)
      throw IllegalAccessException("Error getting user ID from token: $token")
    }
}