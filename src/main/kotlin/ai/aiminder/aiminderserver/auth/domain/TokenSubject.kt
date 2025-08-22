package ai.aiminder.aiminderserver.auth.domain

import java.util.UUID

data class TokenSubject(
  val userId: UUID,
)