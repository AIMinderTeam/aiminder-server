package ai.aiminder.aiminderserver.user.controller

import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.dto.WithdrawUserRequest
import ai.aiminder.aiminderserver.user.service.UserService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
  private val userService: UserService,
) : UserControllerDocs {
  @PatchMapping("/withdraw")
  override suspend fun withdrawUser(
    @RequestBody request: WithdrawUserRequest,
    @AuthenticationPrincipal user: User,
  ): ServiceResponse<Unit> {
    val withdrawalReason = request.getWithdrawalReason()

    userService.withdrawUser(user.id, withdrawalReason)

    return ServiceResponse.from<Unit>(message = "회원 탈퇴가 완료되었습니다.")
  }
}
