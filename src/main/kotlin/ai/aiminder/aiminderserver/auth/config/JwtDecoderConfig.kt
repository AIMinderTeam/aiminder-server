package ai.aiminder.aiminderserver.auth.config

import ai.aiminder.aiminderserver.auth.property.JwtProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder

@Configuration
class JwtDecoderConfig {
  @Bean("accessJwtDecoder")
  fun accessJwtDecoder(jwtProperties: JwtProperties): ReactiveJwtDecoder =
    NimbusReactiveJwtDecoder
      .withSecretKey(jwtProperties.accessTokenSecretKey)
      .macAlgorithm(MacAlgorithm.HS256)
      .build()

  @Bean("refreshJwtDecoder")
  fun refreshJwtDecoder(jwtProperties: JwtProperties): ReactiveJwtDecoder =
    NimbusReactiveJwtDecoder
      .withSecretKey(jwtProperties.refreshTokenSecretKey)
      .macAlgorithm(MacAlgorithm.HS256)
      .build()

  // Backward-compatible composite (optional usage)
  @Bean("reactiveJwtDecoder")
  fun reactiveJwtDecoder(
    @Qualifier("accessJwtDecoder") accessDecoder: ReactiveJwtDecoder,
    @Qualifier("refreshJwtDecoder") refreshDecoder: ReactiveJwtDecoder,
  ): ReactiveJwtDecoder =
    ReactiveJwtDecoder { token ->
      accessDecoder
        .decode(token)
        .onErrorResume { _ -> refreshDecoder.decode(token) }
    }
}