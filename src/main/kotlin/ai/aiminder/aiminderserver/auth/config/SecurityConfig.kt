package ai.aiminder.aiminderserver.auth.config

import ai.aiminder.aiminderserver.auth.property.OAuthProperty
import ai.aiminder.aiminderserver.auth.service.AuthService
import ai.aiminder.aiminderserver.common.util.logger
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.web.server.ServerWebExchange

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val oauthService: AuthService,
    private val oauthProperties: OAuthProperty,
    private val objectMapper: ObjectMapper,
) {
    private val logger = logger()

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .cors { it.disable() }
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/login/**", "/oauth2/**", "/", "/error")
                    .permitAll()
                    .anyExchange()
                    .authenticated()
            }.oauth2Login { oauth2 ->
                oauth2.authenticationSuccessHandler(authenticationSuccessHandler())
            }.exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(customAuthenticationEntryPoint())
            }.build()

    @Bean
    fun customAuthenticationEntryPoint(): ServerAuthenticationEntryPoint =
        ServerAuthenticationEntryPoint { exchange: ServerWebExchange, exception: AuthenticationException ->
            val response: ServerHttpResponse = exchange.response
            response.statusCode = HttpStatus.UNAUTHORIZED
            response.headers.contentType = MediaType.APPLICATION_JSON

            val errorResponse =
                mapOf(
                    "error" to "AUTHENTICATION_REQUIRED",
                    "message" to "인증이 필요합니다. 로그인을 진행해주세요.",
                    "status" to HttpStatus.UNAUTHORIZED.value(),
                    "timestamp" to System.currentTimeMillis(),
                    "path" to exchange.request.path.value(),
                )

            val responseBody = objectMapper.writeValueAsString(errorResponse)
            val buffer: DataBuffer = response.bufferFactory().wrap(responseBody.toByteArray())
            response.writeWith(mono { buffer })
        }

    @Bean
    fun authenticationSuccessHandler(): ServerAuthenticationSuccessHandler =
        ServerAuthenticationSuccessHandler { webFilterExchange: WebFilterExchange, authentication: Authentication ->
            mono {
                val request = webFilterExchange.exchange.request
                val response = webFilterExchange.exchange.response

                try {
                    val token = oauthService.processOAuth2User(authentication, request.path)
                    response.headers.location = oauthProperties.getSuccessUri(token)
                    response.statusCode = HttpStatus.FOUND
                } catch (error: Exception) {
                    logger.error("Authentication success handler error: ${error.message}", error)
                    val response = webFilterExchange.exchange.response
                    response.headers.location = oauthProperties.getErrorUri()
                    response.statusCode = HttpStatus.FOUND
                }
            }.then(webFilterExchange.exchange.response.setComplete())
        }
}
