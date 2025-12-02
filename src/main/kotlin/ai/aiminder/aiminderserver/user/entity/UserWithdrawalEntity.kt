package ai.aiminder.aiminderserver.user.entity

import ai.aiminder.aiminderserver.user.domain.WithdrawalReason
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("user_withdrawals")
data class UserWithdrawalEntity(
  @Id
  @Column("withdrawal_id")
  @get:JvmName("withdrawalId")
  val id: UUID? = null,
  val userId: UUID,
  val reason: WithdrawalReason,
  val createdAt: Instant = Instant.now(),
) : Persistable<UUID> {
  override fun getId(): UUID? = id

  override fun isNew(): Boolean = id == null
}
