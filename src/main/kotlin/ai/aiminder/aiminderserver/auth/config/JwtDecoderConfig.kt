package ai.aiminder.aiminderserver.auth.config

import ai.aiminder.aiminderserver.auth.property.JwtProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder

@Configuration
class JwtDecoderConfig {
  @Bean
  fun reactiveJwtDecoder(jwtProperties: JwtProperties): ReactiveJwtDecoder {
    val accessDecoder =
      NimbusReactiveJwtDecoder
        .withSecretKey(jwtProperties.accessTokenSecretKey)
        .macAlgorithm(MacAlgorithm.HS256)
        .build()

    val refreshDecoder =
      NimbusReactiveJwtDecoder
        .withSecretKey(jwtProperties.refreshTokenSecretKey)
        .macAlgorithm(MacAlgorithm.HS256)
        .build()

    return ReactiveJwtDecoder { token ->
      accessDecoder
        .decode(token)
        .onErrorResume { _ -> refreshDecoder.decode(token) }
    }
  }
}