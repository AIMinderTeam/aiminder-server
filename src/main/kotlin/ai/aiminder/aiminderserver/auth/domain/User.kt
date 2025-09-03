package ai.aiminder.aiminderserver.auth.domain

import ai.aiminder.aiminderserver.auth.entity.UserEntity
import java.time.Instant
import java.util.UUID

data class User(
  val id: UUID,
  val provider: OAuth2Provider,
  val providerId: String,
  val createdAt: Instant,
  val updatedAt: Instant,
) {
  companion object {
    fun from(userEntity: UserEntity): User =
      User(
        id = userEntity.id!!,
        provider = userEntity.provider,
        providerId = userEntity.providerId,
        createdAt = userEntity.createdAt,
        updatedAt = userEntity.updatedAt,
      )
  }
}
