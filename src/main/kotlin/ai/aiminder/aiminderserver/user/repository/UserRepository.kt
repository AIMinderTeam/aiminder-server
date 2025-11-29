package ai.aiminder.aiminderserver.user.repository

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.user.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : CoroutineCrudRepository<UserEntity, UUID> {
  suspend fun findByProviderAndProviderIdAndDeletedAtIsNull(
    provider: OAuth2Provider,
    providerId: String,
  ): UserEntity?

  suspend fun findAllByDeletedAtIsNull(): Flow<UserEntity>
}
