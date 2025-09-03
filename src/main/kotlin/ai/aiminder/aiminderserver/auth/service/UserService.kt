package ai.aiminder.aiminderserver.auth.service

import ai.aiminder.aiminderserver.auth.domain.RefreshToken
import ai.aiminder.aiminderserver.auth.domain.User
import ai.aiminder.aiminderserver.auth.entity.UserEntity
import ai.aiminder.aiminderserver.auth.repository.UserRepository
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

  suspend fun getUserById(id: UUID): UserEntity =
    userRepository.findById(id) ?: throw IllegalAccessException("Not found user $id")
}
