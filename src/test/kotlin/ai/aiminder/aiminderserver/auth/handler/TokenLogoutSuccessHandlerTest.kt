package ai.aiminder.aiminderserver.auth.handler

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import kotlin.test.assertEquals

class TokenLogoutSuccessHandlerTest {
  private val objectMapper = ObjectMapper()

  @Test
  fun `onLogoutSuccess returns 200 JSON`() {
    val handler = TokenLogoutSuccessHandler(objectMapper)
    val req = MockServerHttpRequest.post("/logout").build()
    val exchange = MockServerWebExchange.from(req)
    val chain = WebFilterChain { Mono.empty() }
    val web = WebFilterExchange(exchange, chain)

    handler.onLogoutSuccess(web, TestingAuthenticationToken("u", "p")).block()

    val status = exchange.response.statusCode!!.value()
    val contentType =
      exchange.response.headers.contentType
        ?.toString()
    assertEquals(200, status)
    assertEquals("application/json", contentType)
  }
}