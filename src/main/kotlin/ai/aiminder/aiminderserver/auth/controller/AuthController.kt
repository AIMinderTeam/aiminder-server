package ai.aiminder.aiminderserver.auth.controller

import ai.aiminder.aiminderserver.auth.domain.User
import ai.aiminder.aiminderserver.auth.dto.GetUserResponse
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
        @AuthenticationPrincipal user: User?,
    ): ResponseEntity<GetUserResponse> =
        user
            ?.let { ResponseEntity.ok(GetUserResponse.from(user)) }
            ?: ResponseEntity.status(401).build()
}
