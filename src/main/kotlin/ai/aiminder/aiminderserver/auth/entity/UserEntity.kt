package ai.aiminder.aiminderserver.auth.entity

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("users")
data class UserEntity(
  @Id
  @Column("user_id")
  @get:JvmName("userId")
  val id: UUID? = null,
  val provider: OAuth2Provider,
  val providerId: String,
  val createdAt: Instant = Instant.now(),
  val updatedAt: Instant = createdAt,
) : Persistable<UUID> {
  override fun getId(): UUID? = id

  override fun isNew(): Boolean = id == null
}