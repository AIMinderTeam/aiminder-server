package ai.aiminder.aiminderserver.user.repository

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.user.entity.UserEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : CoroutineCrudRepository<UserEntity, UUID> {
  suspend fun findByProviderAndProviderId(
    provider: OAuth2Provider,
    providerId: String,
  ): UserEntity?
}
