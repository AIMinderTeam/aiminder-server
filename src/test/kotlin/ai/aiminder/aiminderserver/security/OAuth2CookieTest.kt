package ai.aiminder.aiminderserver.security

import ai.aiminder.aiminderserver.auth.domain.TokenGroup
import ai.aiminder.aiminderserver.auth.handler.TokenLoginSuccessHandler
import ai.aiminder.aiminderserver.auth.property.CookieProperties
import ai.aiminder.aiminderserver.auth.service.AuthService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OAuth2CookieTest {
  private val objectMapper = ObjectMapper()

  @Test
  fun `onAuthenticationSuccess sets ACCESS_TOKEN and REFRESH_TOKEN cookies`() {
    // given
    val authService: AuthService = mockk()
    val access = "aaa.bbb.ccc"
    val refresh = "ddd.eee.fff"
    coEvery { authService.processOAuth2User(any(), any()) } returns TokenGroup(access, refresh)

    val cookieProps =
      CookieProperties(
        domain = "",
        sameSite = "Lax",
        httpOnly = true,
        secure = false,
      )
    val handler = TokenLoginSuccessHandler(authService, objectMapper, cookieProps)

    val request = MockServerHttpRequest.get("/oauth2/callback/test").build()
    val exchange = MockServerWebExchange.from(request)
    val chain = WebFilterChain { Mono.empty() }
    val webFilterExchange = WebFilterExchange(exchange, chain)

    val authentication = TestingAuthenticationToken("user", "password")

    // when
    handler.onAuthenticationSuccess(webFilterExchange, authentication).block()

    // then
    val cookies = exchange.response.cookies
    val accessCookie = cookies["ACCESS_TOKEN"]?.firstOrNull()
    val refreshCookie = cookies["REFRESH_TOKEN"]?.firstOrNull()

    assertNotNull(accessCookie, "ACCESS_TOKEN cookie must be present")
    assertNotNull(refreshCookie, "REFRESH_TOKEN cookie must be present")

    assertEquals(access, accessCookie.value)
    assertEquals(refresh, refreshCookie.value)

    // attributes
    assertTrue(accessCookie.isHttpOnly)
    assertTrue(refreshCookie.isHttpOnly)
    assertEquals("/", accessCookie.path)
    assertEquals("/", refreshCookie.path)
    assertEquals(false, accessCookie.isSecure)
    assertEquals(false, refreshCookie.isSecure)
    assertEquals("Lax", accessCookie.sameSite)
    assertEquals("Lax", refreshCookie.sameSite)

    // value format (JWT-like)
    val jwtLike = Regex("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$")
    assertTrue(jwtLike.matches(accessCookie.value))
    assertTrue(jwtLike.matches(refreshCookie.value))
  }
}