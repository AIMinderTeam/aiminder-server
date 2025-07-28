package ai.aiminder.aiminderserver.auth.config

import ai.aiminder.aiminderserver.auth.property.OAuthProperty
import ai.aiminder.aiminderserver.auth.service.AuthService
import ai.aiminder.aiminderserver.common.util.logger
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val oauthService: AuthService,
    private val oauthProperties: OAuthProperty,
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
                // TODO 인증 실패시 실패에 대한 응답을 커스텀해서 응답하도록 수정 필요
            }.oauth2Login { oauth2 ->
                oauth2.authenticationSuccessHandler(authenticationSuccessHandler())
            }.build()

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
