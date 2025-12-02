package ai.aiminder.aiminderserver.inquiry.entity

import ai.aiminder.aiminderserver.inquiry.domain.InquiryStatus
import ai.aiminder.aiminderserver.inquiry.domain.InquiryType
import ai.aiminder.aiminderserver.inquiry.dto.CreateInquiryRequestDto
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("inquiries")
data class InquiryEntity(
  @Id
  @Column("inquiry_id")
  @get:JvmName("inquiryId")
  val id: UUID? = null,
  val userId: UUID,
  val inquiryType: InquiryType,
  val content: String,
  val contactEmail: String? = null,
  val status: InquiryStatus = InquiryStatus.PENDING,
  val createdAt: Instant = Instant.now(),
  val updatedAt: Instant = createdAt,
  val deletedAt: Instant? = null,
) : Persistable<UUID> {
  override fun getId(): UUID? = id

  override fun isNew(): Boolean = id == null

  companion object {
    fun from(dto: CreateInquiryRequestDto): InquiryEntity =
      InquiryEntity(
        userId = dto.userId,
        inquiryType = dto.inquiryType,
        content = dto.content,
        contactEmail = dto.contactEmail,
      )
  }
}
