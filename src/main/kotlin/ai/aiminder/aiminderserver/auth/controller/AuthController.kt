package ai.aiminder.aiminderserver.auth.controller

import ai.aiminder.aiminderserver.common.error.ServiceResponse
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.dto.GetUserResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController : AuthControllerDocs {
  @GetMapping("/me")
  override suspend fun getUser(
    @AuthenticationPrincipal user: User,
  ): ServiceResponse<GetUserResponse> = user.let { ServiceResponse.from(GetUserResponse.from(user)) }
}
