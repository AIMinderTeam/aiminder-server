package ai.aiminder.aiminderserver.auth.config

import ai.aiminder.aiminderserver.auth.domain.User
import ai.aiminder.aiminderserver.auth.property.OAuthProperty
import ai.aiminder.aiminderserver.auth.service.JwtTokenService
import ai.aiminder.aiminderserver.auth.service.OAuth2UserService
import ai.aiminder.aiminderserver.common.util.logger
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val oauth2UserService: OAuth2UserService,
    private val jwtTokenService: JwtTokenService,
    private val oauthProperties: OAuthProperty,
) {
    private val logger = logger()

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/login/**", "/oauth2/**", "/", "/error")
                    .permitAll()
                    .anyExchange()
                    .authenticated()
            }.oauth2Login { oauth2 ->
                oauth2.authenticationSuccessHandler(authenticationSuccessHandler())
            }.build()

    @Bean
    fun authenticationSuccessHandler(): ServerAuthenticationSuccessHandler =
        ServerAuthenticationSuccessHandler { webFilterExchange, authentication ->
            mono {
                try {
                    val oauth2User = authentication.principal as OAuth2User
                    val registrationId =
                        if (authentication is OAuth2LoginAuthenticationToken) {
                            authentication.clientRegistration.registrationId
                        } else {
                            val path =
                                webFilterExchange.exchange.request.path
                                    .value()
                            when {
                                path.contains("google") -> "google"
                                path.contains("kakao") -> "kakao"
                                else -> "unknown"
                            }
                        }

                    val oAuth2User: User = oauth2UserService.processOAuth2User(oauth2User, registrationId)
                    val token = jwtTokenService.generateToken(oAuth2User)
                    val response = webFilterExchange.exchange.response

                    response.headers.location = oauthProperties.getSuccessUri(token)
                    response.statusCode = HttpStatus.FOUND

                    logger.info("OAuth2 authentication successful, redirecting to: ${response.headers.location}")
                } catch (error: Exception) {
                    logger.error("Authentication success handler error: ${error.message}", error)
                    val response = webFilterExchange.exchange.response
                    response.headers.location = oauthProperties.getErrorUri()
                    response.statusCode = HttpStatus.FOUND
                }
            }.then(webFilterExchange.exchange.response.setComplete())
        }
}
