package ai.aiminder.aiminderserver.auth.handler

import ai.aiminder.aiminderserver.common.response.ServiceResponse
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.mono
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class TokenLogoutSuccessHandler(
  private val objectMapper: ObjectMapper,
) : ServerLogoutSuccessHandler {
  override fun onLogoutSuccess(
    webFilterExchange: WebFilterExchange,
    authentication: org.springframework.security.core.Authentication?,
  ): Mono<Void> {
    val response = webFilterExchange.exchange.response
    val responseDto = ServiceResponse.from<Unit>(message = "Logged out")
    response.statusCode = HttpStatusCode.valueOf(responseDto.statusCode)
    response.headers.contentType = MediaType.APPLICATION_JSON
    val responseBody = objectMapper.writeValueAsString(responseDto)
    val buffer: DataBuffer = response.bufferFactory().wrap(responseBody.toByteArray())
    return response.writeWith(mono { buffer })
  }
}
