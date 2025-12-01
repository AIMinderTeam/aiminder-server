package ai.aiminder.aiminderserver.inquiry.dto

import ai.aiminder.aiminderserver.inquiry.domain.InquiryType
import java.util.UUID

data class CreateInquiryRequest(
  val inquiryType: InquiryType,
  val content: String,
  val contactEmail: String? = null,
)

data class CreateInquiryRequestDto(
  val userId: UUID,
  val inquiryType: InquiryType,
  val content: String,
  val contactEmail: String? = null,
)
