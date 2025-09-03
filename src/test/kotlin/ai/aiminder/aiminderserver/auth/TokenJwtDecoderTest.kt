package ai.aiminder.aiminderserver.auth

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.User
import ai.aiminder.aiminderserver.auth.property.JwtProperties
import ai.aiminder.aiminderserver.auth.repository.RefreshTokenRepository
import ai.aiminder.aiminderserver.auth.service.TokenService
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class TokenJwtDecoderTest {
  private fun props(): JwtProperties =
    JwtProperties(
      accessTokenSecret = "test-access-secret-32bytes-min-length-1234",
      accessTokenExpiration = 3600,
      refreshTokenSecret = "test-refresh-secret-32bytes-min-length-5678",
      refreshTokenExpiration = 7200,
    )

  @Test
  fun `issued access token decodes with access decoder`() {
    val jwtProps = props()
    val repo: RefreshTokenRepository = mockk(relaxed = true)
    val svc = TokenService(jwtProps, repo)
    val createdAt = Instant.now()
    val user =
      User(
        id = UUID.randomUUID(),
        provider = OAuth2Provider.GOOGLE,
        providerId = "pid-1",
        createdAt = createdAt,
        updatedAt = createdAt,
      )

    val access = svc.createAccessToken(user)

    val accessDecoder =
      NimbusReactiveJwtDecoder
        .withSecretKey(jwtProps.accessTokenSecretKey)
        .macAlgorithm(MacAlgorithm.HS256)
        .build()

    StepVerifier
      .create(accessDecoder.decode(access))
      .assertNext { jwt -> assertEquals(user.id.toString(), jwt.subject) }
      .verifyComplete()
  }

  @Test
  fun `issued access token fails with refresh decoder`() {
    val jwtProps = props()
    val repo: RefreshTokenRepository = mockk(relaxed = true)
    val svc = TokenService(jwtProps, repo)
    val createdAt = Instant.now()
    val user =
      User(
        id = UUID.randomUUID(),
        provider = OAuth2Provider.GOOGLE,
        providerId = "pid-2",
        createdAt = createdAt,
        updatedAt = createdAt,
      )

    val access = svc.createAccessToken(user)

    val refreshDecoder =
      NimbusReactiveJwtDecoder
        .withSecretKey(jwtProps.refreshTokenSecretKey)
        .macAlgorithm(MacAlgorithm.HS256)
        .build()

    StepVerifier
      .create(refreshDecoder.decode(access))
      .expectError()
      .verify()
  }
}
