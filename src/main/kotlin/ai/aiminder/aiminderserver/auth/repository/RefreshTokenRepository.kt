package ai.aiminder.aiminderserver.auth.repository

import ai.aiminder.aiminderserver.auth.entity.RefreshTokenEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RefreshTokenRepository : CoroutineCrudRepository<RefreshTokenEntity, UUID> {
  suspend fun findByUserId(userId: UUID): RefreshTokenEntity?

  suspend fun deleteByUserId(userId: UUID)
}
