package ai.aiminder.aiminderserver.user.repository

import ai.aiminder.aiminderserver.user.entity.UserWithdrawalEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserWithdrawalRepository : CoroutineCrudRepository<UserWithdrawalEntity, UUID> {
  suspend fun findByUserId(userId: UUID): UserWithdrawalEntity?
}
