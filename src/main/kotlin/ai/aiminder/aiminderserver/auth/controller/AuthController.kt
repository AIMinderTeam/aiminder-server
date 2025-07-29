package ai.aiminder.aiminderserver.auth.controller

import ai.aiminder.aiminderserver.auth.domain.User
import ai.aiminder.aiminderserver.auth.dto.GetUserResponse
import ai.aiminder.aiminderserver.auth.dto.ValidateTokenResponse
import ai.aiminder.aiminderserver.auth.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
) {
    @GetMapping("/user")
    suspend fun getUser(
        @AuthenticationPrincipal user: User?,
    ): ResponseEntity<GetUserResponse> =
        user
            ?.let { ResponseEntity.ok(GetUserResponse.from(user)) }
            ?: ResponseEntity.status(401).build()

    @PostMapping("/validate")
    suspend fun validateToken(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<ValidateTokenResponse> {
        val jwtToken = token.removePrefix("Bearer ")
        val user: User? = userService.getUser(jwtToken)
        return if (user != null) {
            ResponseEntity.ok(ValidateTokenResponse(user))
        } else {
            ResponseEntity.status(401).build()
        }
    }
}
