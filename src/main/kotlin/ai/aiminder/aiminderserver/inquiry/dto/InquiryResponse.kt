package ai.aiminder.aiminderserver.inquiry.dto

import ai.aiminder.aiminderserver.inquiry.domain.Inquiry
import ai.aiminder.aiminderserver.inquiry.domain.InquiryStatus
import ai.aiminder.aiminderserver.inquiry.domain.InquiryType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "문의 응답 데이터")
data class InquiryResponse(
  @Schema(description = "문의 고유 ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
  val id: UUID,
  @Schema(description = "사용자 ID", example = "2f6a3a4c-1c3b-4bde-9d2a-6c2c8b6a1e7f")
  val userId: UUID,
  @Schema(description = "문의 유형", example = "BUG_REPORT")
  val inquiryType: InquiryType,
  @Schema(description = "문의 내용", example = "앱이 자주 크래시됩니다.")
  val content: String,
  @Schema(description = "연락 이메일", example = "user@example.com")
  val contactEmail: String?,
  @Schema(description = "문의 상태", example = "PENDING")
  val status: InquiryStatus,
  @Schema(description = "생성일시", example = "2024-01-01T00:00:00Z")
  val createdAt: Instant,
  @Schema(description = "수정일시", example = "2024-01-01T00:00:00Z")
  val updatedAt: Instant,
  @Schema(description = "삭제일시", example = "null")
  val deletedAt: Instant?,
) {
  companion object {
    fun from(inquiry: Inquiry): InquiryResponse =
      InquiryResponse(
        id = inquiry.id,
        userId = inquiry.userId,
        inquiryType = inquiry.inquiryType,
        content = inquiry.content,
        contactEmail = inquiry.contactEmail,
        status = inquiry.status,
        createdAt = inquiry.createdAt,
        updatedAt = inquiry.updatedAt,
        deletedAt = inquiry.deletedAt,
      )
  }
}
