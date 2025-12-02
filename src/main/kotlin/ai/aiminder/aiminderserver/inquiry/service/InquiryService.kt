package ai.aiminder.aiminderserver.inquiry.service

import ai.aiminder.aiminderserver.inquiry.domain.Inquiry
import ai.aiminder.aiminderserver.inquiry.dto.CreateInquiryRequestDto
import ai.aiminder.aiminderserver.inquiry.dto.InquiryResponse
import ai.aiminder.aiminderserver.inquiry.entity.InquiryEntity
import ai.aiminder.aiminderserver.inquiry.repository.InquiryRepository
import org.springframework.stereotype.Service

@Service
class InquiryService(
  private val inquiryRepository: InquiryRepository,
) {
  suspend fun create(dto: CreateInquiryRequestDto): InquiryResponse {
    val inquiry =
      InquiryEntity
        .from(dto)
        .let { inquiryRepository.save(it) }
        .let { Inquiry.fromEntity(it) }

    return InquiryResponse.from(inquiry)
  }
}
