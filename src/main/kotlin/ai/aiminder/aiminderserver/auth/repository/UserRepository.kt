package ai.aiminder.aiminderserver.auth.repository

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.User
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : CoroutineCrudRepository<User, UUID> {
  suspend fun findByProviderAndProviderId(
    provider: OAuth2Provider,
    providerId: String,
  ): User?
}