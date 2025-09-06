package ai.aiminder.aiminderserver.user.service

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.RefreshToken
import ai.aiminder.aiminderserver.auth.dto.OAuth2UserInfo
import ai.aiminder.aiminderserver.auth.service.TokenService
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.entity.UserEntity
import ai.aiminder.aiminderserver.user.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
  private val tokenService: TokenService,
  private val userRepository: UserRepository,
) {
  suspend fun getUser(token: RefreshToken): User {
    val userId = tokenService.getUserIdFromToken(token)
    val userEntity: UserEntity =
      userRepository.findById(userId) ?: throw IllegalAccessException("Not found user $userId")
    return User.from(userEntity)
  }

  suspend fun getUser(
    provider: OAuth2Provider,
    providerId: String,
  ): User? {
    val userEntity: UserEntity? =
      userRepository.findByProviderAndProviderId(provider, providerId)
    return userEntity?.let { User.from(it) }
  }

  suspend fun getUserById(id: UUID): UserEntity =
    userRepository.findById(id) ?: throw IllegalAccessException("Not found user $id")

  suspend fun createUser(
    userInfo: OAuth2UserInfo,
    provider: String,
  ): User {
    val newUserEntity =
      UserEntity(
        provider = OAuth2Provider.from(provider),
        providerId = userInfo.id,
      )

    val savedUser = userRepository.save(newUserEntity)

    return User.from(savedUser)
  }
}
