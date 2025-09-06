package ai.aiminder.aiminderserver.auth.domain

import ai.aiminder.aiminderserver.auth.error.AuthError

enum class OAuth2Provider {
  GOOGLE,
  KAKAO,
  ;

  companion object {
    fun from(registrationId: String): OAuth2Provider =
      OAuth2Provider.entries.find { it.name == registrationId.uppercase() }
        ?: throw AuthError.UnsupportedProvider("Unsupported provider: $registrationId")
  }
}
