package ai.aiminder.aiminderserver.auth.service

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.TokenGroup
import ai.aiminder.aiminderserver.auth.dto.OAuth2UserInfo
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.user.service.UserService
import org.springframework.http.server.RequestPath
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class AuthService(
  private val tokenService: TokenService,
  private val userService: UserService,
) {
  suspend fun processOAuth2User(
    authentication: Authentication,
    requestPath: RequestPath,
  ): TokenGroup {
    val oauth2User = authentication.principal as OAuth2User
    val registrationId: String =
      if (authentication is OAuth2AuthenticationToken) {
        authentication.authorizedClientRegistrationId
      } else {
        val path = requestPath.value()
        when {
          path.contains("google") -> "google"
          path.contains("kakao") -> "kakao"
          else -> "unknown"
        }
      }
    val oauthInfo: OAuth2UserInfo = extractUserInfo(oauth2User, registrationId)
    val provider: OAuth2Provider = OAuth2Provider.from(registrationId)
    val user =
      userService.getUser(provider, oauthInfo.id)
        ?: userService.createUser(oauthInfo, provider.name)
    val tokenGroup: TokenGroup = tokenService.createTokenGroup(user)
    return tokenGroup
  }

  private fun extractUserInfo(
    oauth2User: OAuth2User,
    registrationId: String,
  ): OAuth2UserInfo =
    when (registrationId.uppercase()) {
      OAuth2Provider.GOOGLE.name -> extractGoogleUserInfo(oauth2User)
      OAuth2Provider.KAKAO.name -> extractKakaoUserInfo(oauth2User)
      else -> throw AuthError.UnsupportedProvider("Unsupported provider: $registrationId")
    }

  private fun extractGoogleUserInfo(oauth2User: OAuth2User): OAuth2UserInfo =
    OAuth2UserInfo(
      id = oauth2User.getAttribute("sub") ?: throw AuthError.NotFoundOAuthId(),
    )

  private fun extractKakaoUserInfo(oauth2User: OAuth2User): OAuth2UserInfo =
    OAuth2UserInfo(
      id = oauth2User.getAttribute<Long>("id")?.toString() ?: throw AuthError.NotFoundOAuthId(),
    )
}
