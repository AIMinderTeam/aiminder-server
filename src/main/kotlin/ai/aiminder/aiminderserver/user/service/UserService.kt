package ai.aiminder.aiminderserver.user.service

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.RefreshToken
import ai.aiminder.aiminderserver.auth.dto.OAuth2UserInfo
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.auth.service.TokenService
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.domain.UserWithdrawal
import ai.aiminder.aiminderserver.user.domain.WithdrawalReason
import ai.aiminder.aiminderserver.user.entity.UserEntity
import ai.aiminder.aiminderserver.user.entity.UserWithdrawalEntity
import ai.aiminder.aiminderserver.user.repository.UserRepository
import ai.aiminder.aiminderserver.user.repository.UserWithdrawalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
  private val tokenService: TokenService,
  private val userRepository: UserRepository,
  private val userWithdrawalRepository: UserWithdrawalRepository,
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

  suspend fun withdrawUser(
    userId: UUID,
    withdrawalReason: WithdrawalReason?,
  ): UserWithdrawal? {
    val userEntity = getUserById(userId)

    val updatedUser =
      userEntity.copy(
        deletedAt = java.time.Instant.now(),
        updatedAt = java.time.Instant.now(),
      )

    userRepository.save(updatedUser)

    if (withdrawalReason != null) {
      val withdrawalEntity =
        UserWithdrawalEntity(
          userId = userId,
          reason = withdrawalReason,
        )

      val savedWithdrawal = userWithdrawalRepository.save(withdrawalEntity)
      return UserWithdrawal.from(savedWithdrawal)
    }

    return null
  }
}
