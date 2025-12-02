package ai.aiminder.aiminderserver.inquiry.error

import ai.aiminder.aiminderserver.common.error.ServiceError
import org.springframework.http.HttpStatus

sealed class InquiryError(
  override val status: HttpStatus,
  override val message: String,
) : ServiceError() {
  override val mainCode: String = "INQUIRY"

  data class InvalidContent(val content: String) : InquiryError(
    HttpStatus.BAD_REQUEST,
    "문의 내용이 유효하지 않습니다. 내용은 1자 이상 1000자 이하여야 합니다.",
  )

  data class InvalidEmail(val email: String) : InquiryError(
    HttpStatus.BAD_REQUEST,
    "이메일 형식이 올바르지 않습니다.",
  )
}
