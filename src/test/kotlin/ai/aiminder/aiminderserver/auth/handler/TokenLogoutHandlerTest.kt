package ai.aiminder.aiminderserver.auth.handler

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.entity.UserEntity
import ai.aiminder.aiminderserver.auth.property.CookieProperties
import ai.aiminder.aiminderserver.auth.repository.RefreshTokenRepository
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TokenLogoutHandlerTest {
  private val repo: RefreshTokenRepository = mockk(relaxed = true)
  private val cookieProps = CookieProperties(domain = "", sameSite = "Lax", httpOnly = true, secure = false)

  @Test
  fun `로그아웃 시 리프레시 토큰 삭제 및 만료 쿠키 설정`() {
    // given
    val handler = TokenLogoutHandler(repo, cookieProps)
    val req = MockServerHttpRequest.post("/logout").build()
    val exchange = MockServerWebExchange.from(req)
    val chain = WebFilterChain { Mono.empty() }
    val webExchange = WebFilterExchange(exchange, chain)

    val userId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val principal = UserEntity(id = userId, provider = OAuth2Provider.GOOGLE, providerId = "pid")
    val authentication = UsernamePasswordAuthenticationToken(principal, null)

    // when
    handler.logout(webExchange, authentication).block()

    // then: repo delete invoked
    coVerify(exactly = 1) { repo.deleteByUserId(userId) }

    // then: cookies cleared
    val access = exchange.response.cookies["ACCESS_TOKEN"]?.firstOrNull()
    val refresh = exchange.response.cookies["REFRESH_TOKEN"]?.firstOrNull()
    assertNotNull(access)
    assertNotNull(refresh)
    assertEquals(0, access.maxAge.seconds)
    assertEquals(0, refresh.maxAge.seconds)
    assertEquals("/", access.path)
    assertEquals("Lax", access.sameSite)
  }

  @Test
  fun `주체 없음 또는 ID 없음이어도 쿠키는 제거된다`() {
    val handler = TokenLogoutHandler(repo, cookieProps)
    val req = MockServerHttpRequest.post("/logout").build()
    val exchange = MockServerWebExchange.from(req)
    val chain = WebFilterChain { Mono.empty() }
    val webExchange = WebFilterExchange(exchange, chain)

    val principal = UserEntity(id = null, provider = OAuth2Provider.GOOGLE, providerId = "pid")
    val authentication = UsernamePasswordAuthenticationToken(principal, null)

    handler.logout(webExchange, authentication).block()

    val access = exchange.response.cookies["ACCESS_TOKEN"]?.firstOrNull()
    val refresh = exchange.response.cookies["REFRESH_TOKEN"]?.firstOrNull()
    assertNotNull(access)
    assertNotNull(refresh)
    assertEquals(0, access.maxAge.seconds)
    assertEquals(0, refresh.maxAge.seconds)
  }
}