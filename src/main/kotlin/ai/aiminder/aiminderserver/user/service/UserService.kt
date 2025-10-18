package ai.aiminder.aiminderserver.user.service

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.RefreshToken
import ai.aiminder.aiminderserver.auth.dto.OAuth2UserInfo
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.auth.service.TokenService
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.entity.UserEntity
import ai.aiminder.aiminderserver.user.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
  private val tokenService: TokenService,
  private val userRepository: UserRepository,
) {
  suspend fun getUser(token: RefreshToken): User {
    val userId = tokenService.getUserIdFromRefreshToken(token)
    val userEntity: UserEntity = getUserById(userId)
    return User.from(userEntity)
  }

  suspend fun getUser(
    provider: OAuth2Provider,
    providerId: String,
  ): User? {
    val userEntity: UserEntity? =
      userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(provider, providerId)
    return userEntity?.let { User.from(it) }
  }

  suspend fun getUserById(id: UUID): UserEntity =
    userRepository
      .findById(id)
      ?.takeIf { it.deletedAt == null }
      ?: throw AuthError.UserNotFound(id)

  suspend fun getUsers(): Flow<User> =
    userRepository
      .findAllByDeletedAtIsNull()
      .map { User.from(it) }

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
