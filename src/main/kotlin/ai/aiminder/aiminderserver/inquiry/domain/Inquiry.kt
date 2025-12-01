package ai.aiminder.aiminderserver.inquiry.domain

import ai.aiminder.aiminderserver.inquiry.entity.InquiryEntity
import java.time.Instant
import java.util.UUID

data class Inquiry(
  val id: UUID,
  val userId: UUID,
  val inquiryType: InquiryType,
  val content: String,
  val contactEmail: String?,
  val status: InquiryStatus,
  val createdAt: Instant,
  val updatedAt: Instant,
  val deletedAt: Instant?,
) {
  companion object {
    fun fromEntity(inquiryEntity: InquiryEntity): Inquiry =
      Inquiry(
        id = inquiryEntity.id!!,
        userId = inquiryEntity.userId,
        inquiryType = inquiryEntity.inquiryType,
        content = inquiryEntity.content,
        contactEmail = inquiryEntity.contactEmail,
        status = inquiryEntity.status,
        createdAt = inquiryEntity.createdAt,
        updatedAt = inquiryEntity.updatedAt,
        deletedAt = inquiryEntity.deletedAt,
      )
  }
}
