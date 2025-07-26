package ai.aiminder.aiminderserver.auth.dto

import org.springframework.security.oauth2.core.user.OAuth2User

data class GetUserResponse(
    val name: String,
) {
    companion object {
        fun from(oauth2User: OAuth2User): GetUserResponse =
            GetUserResponse(
                name = oauth2User.name,
            )
    }
}
