package ai.aiminder.aiminderserver.user.domain

import ai.aiminder.aiminderserver.user.entity.UserWithdrawalEntity
import java.time.Instant
import java.util.UUID

data class UserWithdrawal(
  val id: UUID,
  val userId: UUID,
  val reason: WithdrawalReason,
  val createdAt: Instant,
) {
  companion object {
    fun from(entity: UserWithdrawalEntity): UserWithdrawal =
      UserWithdrawal(
        id = entity.id!!,
        userId = entity.userId,
        reason = entity.reason,
        createdAt = entity.createdAt,
      )
  }
}
