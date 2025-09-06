package ai.aiminder.aiminderserver.auth.service

import ai.aiminder.aiminderserver.auth.domain.AccessToken
import ai.aiminder.aiminderserver.auth.domain.RefreshToken
import ai.aiminder.aiminderserver.auth.domain.TokenGroup
import ai.aiminder.aiminderserver.auth.entity.RefreshTokenEntity
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.auth.property.JwtProperties
import ai.aiminder.aiminderserver.auth.repository.RefreshTokenRepository
import ai.aiminder.aiminderserver.common.util.logger
import ai.aiminder.aiminderserver.common.util.toUUID
import ai.aiminder.aiminderserver.user.domain.User
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class TokenService(
  private val jwtProperties: JwtProperties,
  private val refreshTokenRepository: RefreshTokenRepository,
) {
  private val accessTokenKey = jwtProperties.accessTokenSecretKey
  private val refreshTokenKey = jwtProperties.refreshTokenSecretKey
  private val logger = logger()

  fun createAccessToken(user: User): AccessToken {
    val now = Instant.now()
    val expireDate = jwtProperties.addAccessTokenExpiration(now)
    val key = accessTokenKey

    return createToken(user = user, now = now, expireDate = expireDate, key = key)
  }

  suspend fun createRefreshToken(user: User): RefreshToken {
    val now = Instant.now()
    val expireDate = jwtProperties.addRefreshTokenExpiration(now)
    val key = refreshTokenKey
    val token = createToken(user, now, expireDate, key)
    refreshTokenRepository
      .findByUserId(user.id)
      .let { refreshToken ->
        refreshToken
          ?.update(token)
          ?: RefreshTokenEntity(userId = user.id, token = token)
      }.let { refreshToken -> refreshTokenRepository.save(refreshToken) }
    return token
  }

  @Transactional
  suspend fun createTokenGroup(user: User): TokenGroup {
    val accessToken: AccessToken = createAccessToken(user)
    val refreshToken: RefreshToken = createRefreshToken(user)
    return TokenGroup(accessToken, refreshToken)
  }

  fun validateAccessToken(token: String): Boolean =
    runCatching {
      Jwts
        .parser()
        .verifyWith(accessTokenKey)
        .build()
        .parseSignedClaims(token)
      true
    }.getOrElse {
      logger.error("Invalid JWT accessToken: ${it.message}", it)
      false
    }

  suspend fun validateRefreshToken(token: String): Boolean =
    runCatching {
      val userId: UUID = getUserIdFromToken(token)
      val foundRefreshToken: RefreshTokenEntity =
        refreshTokenRepository
          .findByUserId(userId)
          ?: throw AuthError.InvalidRefreshToken()
      foundRefreshToken
        .takeIf { it.token == token }
        ?: throw AuthError.InvalidRefreshToken()
      true
    }.getOrElse {
      logger.error("Invalid JWT refreshToken: ${it.message}", it)
      false
    }

  fun getUserIdFromToken(token: String): UUID =
    runCatching {
      Jwts
        .parser()
        .verifyWith(accessTokenKey)
        .build()
        .parseSignedClaims(token)
        .payload
        .subject
        .toUUID()
    }.getOrElse {
      logger.error("Error getting user ID from token: ${it.message}", it)
      throw AuthError.InvalidAccessToken()
    }

  private fun createToken(
    user: User,
    now: Instant?,
    expireDate: Instant,
    key: SecretKey,
  ): String =
    Jwts
      .builder()
      .subject(user.id.toString())
      .claim("provider", user.provider)
      .issuedAt(Date.from(now))
      .expiration(Date.from(expireDate))
      .signWith(key, Jwts.SIG.HS256)
      .compact()
}
