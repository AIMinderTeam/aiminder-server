package ai.aiminder.aiminderserver.auth.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
  @Bean
  fun openApi(): OpenAPI =
    OpenAPI()
      .components(
        Components()
          .addSecuritySchemes(
            "bearerAuth",
            SecurityScheme()
              .type(SecurityScheme.Type.HTTP)
              .scheme("bearer")
              .bearerFormat("JWT")
              .description("JWT 토큰을 입력하세요. 'Bearer '는 자동으로 추가됩니다."),
          ),
      ).addSecurityItem(
        SecurityRequirement().addList("bearerAuth"),
      )
}
