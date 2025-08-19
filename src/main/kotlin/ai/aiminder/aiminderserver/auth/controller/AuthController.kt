package ai.aiminder.aiminderserver.auth.controller

import ai.aiminder.aiminderserver.auth.dto.GetUserResponse
import ai.aiminder.aiminderserver.auth.entity.UserEntity
import org.springframework.http.ResponseEntity
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
  ): ResponseEntity<GetUserResponse> =
    userEntity
      ?.let { ResponseEntity.ok(GetUserResponse.from(userEntity)) }
      ?: ResponseEntity.status(401).build()
}