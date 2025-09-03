package ai.aiminder.aiminderserver.auth.domain

import ai.aiminder.aiminderserver.auth.entity.RefreshToken

typealias AccessToken = String
typealias RefreshToken = String

data class TokenGroup(
  val accessToken: AccessToken,
  val refreshToken: RefreshToken,
)
