package ai.aiminder.aiminderserver.user.dto

import ai.aiminder.aiminderserver.user.domain.WithdrawalReason
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원 탈퇴 요청")
data class WithdrawUserRequest(
  @Schema(description = "탈퇴 사유", example = "서비스가 마음에 들지 않음", required = false)
  val reason: String? = null,
) {
  fun getWithdrawalReason(): WithdrawalReason? = reason?.let { WithdrawalReason.fromDisplayName(it) }
}
