package ai.aiminder.aiminderserver.auth.service

import ai.aiminder.aiminderserver.auth.domain.User
import ai.aiminder.aiminderserver.auth.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
  private val jwtTokenService: JwtTokenService,
  private val userRepository: UserRepository,
) {
  suspend fun getUser(token: String): User? {
    val userId = jwtTokenService.getUserIdFromToken(token)
    val user: User? = userRepository.findById(userId)
    return user
  }
}