package ai.aiminder.aiminderserver.auth.controller

import ai.aiminder.aiminderserver.auth.domain.User
import ai.aiminder.aiminderserver.auth.dto.GetUserResponse
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.common.error.Response
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController : AuthControllerDocs {
  @GetMapping("/user")
  override suspend fun getUser(
    @AuthenticationPrincipal user: User?,
  ): Response<GetUserResponse> =
    user
      ?.let { Response.from(GetUserResponse.from(user)) }
      ?: run { Response.from(AuthError.UNAUTHORIZED) }
}