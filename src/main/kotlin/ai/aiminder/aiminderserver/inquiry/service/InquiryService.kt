package ai.aiminder.aiminderserver.inquiry.service

import ai.aiminder.aiminderserver.inquiry.domain.Inquiry
import ai.aiminder.aiminderserver.inquiry.dto.CreateInquiryRequestDto
import ai.aiminder.aiminderserver.inquiry.dto.InquiryResponse
import ai.aiminder.aiminderserver.inquiry.entity.InquiryEntity
import ai.aiminder.aiminderserver.inquiry.error.InquiryError
import ai.aiminder.aiminderserver.inquiry.repository.InquiryRepository
import org.springframework.stereotype.Service

@Service
class InquiryService(
  private val inquiryRepository: InquiryRepository,
) {
  suspend fun create(dto: CreateInquiryRequestDto): InquiryResponse {
    // 입력 검증
    validateInquiryContent(dto.content)
    dto.contactEmail?.let { validateEmail(it) }

    val inquiry =
      InquiryEntity
        .from(dto)
        .let { inquiryRepository.save(it) }
        .let { Inquiry.fromEntity(it) }

    return InquiryResponse.from(inquiry)
  }

  private fun validateInquiryContent(content: String) {
    if (content.isBlank() || content.length > 1000) {
      throw InquiryError.InvalidContent(content)
    }
  }

  private fun validateEmail(email: String) {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    if (!email.matches(emailRegex.toRegex())) {
      throw InquiryError.InvalidEmail(email)
    }
  }
}
