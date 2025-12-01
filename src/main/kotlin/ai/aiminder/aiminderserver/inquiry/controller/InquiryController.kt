package ai.aiminder.aiminderserver.inquiry.controller

import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.inquiry.dto.CreateInquiryRequest
import ai.aiminder.aiminderserver.inquiry.dto.CreateInquiryRequestDto
import ai.aiminder.aiminderserver.inquiry.dto.InquiryResponse
import ai.aiminder.aiminderserver.inquiry.service.InquiryService
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/inquiries")
class InquiryController(
  private val inquiryService: InquiryService,
) : InquiryControllerDocs {
  @PostMapping
  override suspend fun createInquiry(
    @RequestBody
    request: CreateInquiryRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<InquiryResponse> =
    inquiryService
      .create(
        CreateInquiryRequestDto(
          userId = user.id,
          inquiryType = request.inquiryType,
          content = request.content,
          contactEmail = request.contactEmail,
        ),
      ).let { inquiry -> ServiceResponse.from(inquiry) }
}
