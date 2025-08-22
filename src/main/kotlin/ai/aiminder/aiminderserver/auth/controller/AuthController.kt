package ai.aiminder.aiminderserver.auth.controller

import ai.aiminder.aiminderserver.auth.dto.GetUserResponse
import ai.aiminder.aiminderserver.auth.entity.UserEntity
import ai.aiminder.aiminderserver.auth.error.AuthError
import ai.aiminder.aiminderserver.common.error.Response
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController {
  @GetMapping("/user")
  suspend fun getUser(
    @AuthenticationPrincipal userEntity: UserEntity?,
  ): Response<GetUserResponse> =
    userEntity
      ?.let { Response.from(GetUserResponse.from(userEntity)) }
      ?: run { Response.from(AuthError.UNAUTHORIZED) }
}