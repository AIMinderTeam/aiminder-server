package ai.aiminder.aiminderserver.auth.entity

import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

typealias RefreshToken = String

@Table(value = "refresh_token")
data class RefreshTokenEntity(
  @Id
  @Column("refresh_token_id")
  @get:JvmName("refreshTokenId")
  val id: UUID? = null,
  val userId: UUID,
  val token: RefreshToken,
  val createdAt: Instant = Instant.now(),
  val updatedAt: Instant = createdAt,
) : Persistable<UUID> {
  override fun getId(): UUID? = id

  override fun isNew(): Boolean = id == null

  fun update(token: RefreshToken): RefreshTokenEntity =
    this.copy(
      token = token,
      updatedAt = Instant.now(),
    )
}
