package ai.aiminder.aiminderserver.auth.filter

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.entity.UserEntity
import ai.aiminder.aiminderserver.auth.property.CookieProperties
import ai.aiminder.aiminderserver.auth.service.TokenService
import ai.aiminder.aiminderserver.auth.service.UserService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CookieAuthenticationWebFilterTest {
  private val accessDecoder: ReactiveJwtDecoder = mockk()
  private val refreshDecoder: ReactiveJwtDecoder = mockk()
  private val tokenService: TokenService = mockk()
  private val userService: UserService = mockk()
  private val cookieProps = CookieProperties(domain = "", sameSite = "Lax", httpOnly = true, secure = false)

  private fun jwt(token: String): Jwt =
    Jwt
      .withTokenValue(token)
      .header("alg", "none")
      .claim("sub", "00000000-0000-0000-0000-000000000000")
      .build()

  @Test
  fun `유효한 액세스 토큰으로 인증 컨텍스트가 설정된다`() {
    // given
    val access = "aaa.bbb.ccc"
    every { accessDecoder.decode(access) } returns Mono.just(jwt(access))
    every { tokenService.validateAccessToken(access) } returns true
    val subjectId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val user = UserEntity(id = subjectId, provider = OAuth2Provider.GOOGLE, providerId = "123")
    coEvery { userService.getUserById(subjectId) } returns user

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=$access")
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticated = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder
          .getContext()
          .doOnNext { authenticated = true }
          .then()
      }

    val filter = CookieAuthenticationWebFilter(accessDecoder, refreshDecoder, tokenService, userService, cookieProps)

    // when
    filter.filter(exchange, chain).block()

    // then
    assertEquals(true, authenticated)
  }

  @Test
  fun `액세스 토큰 무효, 리프래시 유효 시 재발급되고 응답 쿠키가 설정된다`() {
    // given
    val access = "bad.access"
    val refresh = "good.refresh"
    val newAccess = "new.access"
    val newRefresh = "new.refresh"

    every { accessDecoder.decode(access) } returns Mono.error(RuntimeException("invalid"))
    every { refreshDecoder.decode(refresh) } returns Mono.just(jwt(refresh))
    coEvery { tokenService.validateRefreshToken(refresh) } returns true

    val subjectId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val user = UserEntity(id = subjectId, provider = OAuth2Provider.GOOGLE, providerId = "123")
    coEvery { userService.getUser(refresh) } returns user
    every { tokenService.createAccessToken(user) } returns newAccess
    coEvery { tokenService.createRefreshToken(user) } returns newRefresh
    every { accessDecoder.decode(newAccess) } returns Mono.just(jwt(newAccess))
    coEvery { userService.getUserById(subjectId) } returns user

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=$access; REFRESH_TOKEN=$refresh")
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticated = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder
          .getContext()
          .doOnNext { authenticated = true }
          .then()
      }

    val filter = CookieAuthenticationWebFilter(accessDecoder, refreshDecoder, tokenService, userService, cookieProps)

    // when
    filter.filter(exchange, chain).block()

    // then
    val accessCookie = exchange.response.cookies["ACCESS_TOKEN"]?.firstOrNull()
    val refreshCookie = exchange.response.cookies["REFRESH_TOKEN"]?.firstOrNull()
    assertNotNull(accessCookie)
    assertNotNull(refreshCookie)
    assertEquals(newAccess, accessCookie.value)
    assertEquals(newRefresh, refreshCookie.value)
    assertEquals(true, authenticated)
  }

  @Test
  fun `액세스 무효, 리프래시 무효 시 만료 쿠키로 제거된다`() {
    // given
    val access = "bad.access"
    val refresh = "bad.refresh"
    every { accessDecoder.decode(access) } returns Mono.error(RuntimeException("invalid"))
    every { refreshDecoder.decode(refresh) } returns Mono.error(RuntimeException("invalid"))

    val request =
      MockServerHttpRequest
        .get("/api/test")
        .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=$access; REFRESH_TOKEN=$refresh")
        .build()
    val exchange = MockServerWebExchange.from(request)

    var authenticationSet = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder
          .getContext()
          .doOnNext { authenticationSet = true }
          .then()
      }

    val filter = CookieAuthenticationWebFilter(accessDecoder, refreshDecoder, tokenService, userService, cookieProps)

    // when
    filter.filter(exchange, chain).block()

    // then
    val accessCookie = exchange.response.cookies["ACCESS_TOKEN"]?.firstOrNull()
    val refreshCookie = exchange.response.cookies["REFRESH_TOKEN"]?.firstOrNull()
    assertNotNull(accessCookie)
    assertNotNull(refreshCookie)
    assertEquals(0, accessCookie.maxAge.seconds)
    assertEquals(0, refreshCookie.maxAge.seconds)
    assertEquals(false, authenticationSet)
  }

  @Test
  fun `쿠키 미제공 시 체인만 통과한다`() {
    // given
    val request = MockServerHttpRequest.get("/api/test").build()
    val exchange = MockServerWebExchange.from(request)

    var authenticationSet = false
    val chain =
      WebFilterChain { _ ->
        ReactiveSecurityContextHolder
          .getContext()
          .doOnNext { authenticationSet = true }
          .then()
      }

    val filter = CookieAuthenticationWebFilter(accessDecoder, refreshDecoder, tokenService, userService, cookieProps)

    // when
    filter.filter(exchange, chain).block()

    // then
    assertEquals(false, authenticationSet)
  }
}