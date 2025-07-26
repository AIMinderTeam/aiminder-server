package ai.aiminder.aiminderserver.auth.domain

enum class OAuth2Provider {
    GOOGLE,
    KAKAO,
    ;

    companion object {
        fun from(registrationId: String): OAuth2Provider =
            OAuth2Provider.entries.find { it.name == registrationId.uppercase() }
                ?: throw IllegalArgumentException("Unsupported provider: $registrationId")
    }
}
