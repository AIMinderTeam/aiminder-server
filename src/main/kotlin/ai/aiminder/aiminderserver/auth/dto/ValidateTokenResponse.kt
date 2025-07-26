package ai.aiminder.aiminderserver.auth.dto

import ai.aiminder.aiminderserver.auth.domain.User

data class ValidateTokenResponse(
    val user: User,
)
