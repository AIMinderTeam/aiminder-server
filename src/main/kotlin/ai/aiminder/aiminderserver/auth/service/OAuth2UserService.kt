package ai.aiminder.aiminderserver.auth.service

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.User
import ai.aiminder.aiminderserver.auth.dto.OAuth2UserInfo
import ai.aiminder.aiminderserver.auth.repository.UserRepository
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class OAuth2UserService(
    private val userRepository: UserRepository,
) {
    suspend fun processOAuth2User(
        oauth2User: OAuth2User,
        providerId: String,
    ): User {
        val oauthInfo: OAuth2UserInfo = extractUserInfo(oauth2User, providerId)
        val provider = OAuth2Provider.from(providerId)

        return userRepository
            .findByProviderAndProviderId(
                provider = provider,
                providerId = oauthInfo.id,
            ) ?: createNewUser(userInfo = oauthInfo, provider = providerId)
    }

    private suspend fun createNewUser(
        userInfo: OAuth2UserInfo,
        provider: String,
    ): User {
        val newUser =
            User(
                provider = OAuth2Provider.from(provider),
                providerId = userInfo.id,
            )

        return userRepository.save(newUser)
    }

    private fun extractUserInfo(
        oauth2User: OAuth2User,
        registrationId: String,
    ): OAuth2UserInfo =
        when (registrationId.uppercase()) {
            OAuth2Provider.GOOGLE.name -> extractGoogleUserInfo(oauth2User)
            OAuth2Provider.KAKAO.name -> extractKakaoUserInfo(oauth2User)
            else -> throw IllegalArgumentException("Unsupported provider: $registrationId")
        }

    private fun extractGoogleUserInfo(oauth2User: OAuth2User): OAuth2UserInfo =
        OAuth2UserInfo(
            id = oauth2User.getAttribute("sub") ?: "",
        )

    private fun extractKakaoUserInfo(oauth2User: OAuth2User): OAuth2UserInfo =
        OAuth2UserInfo(
            id = oauth2User.getAttribute<Long>("id")?.toString() ?: "",
        )
}
