package ai.aiminder.aiminderserver.inquiry.controller

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.inquiry.domain.InquiryStatus
import ai.aiminder.aiminderserver.inquiry.domain.InquiryType
import ai.aiminder.aiminderserver.inquiry.dto.CreateInquiryRequest
import ai.aiminder.aiminderserver.inquiry.dto.InquiryResponse
import ai.aiminder.aiminderserver.inquiry.repository.InquiryRepository
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.entity.UserEntity
import ai.aiminder.aiminderserver.user.repository.UserRepository
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication
import org.springframework.test.web.reactive.server.expectBody

class InquiryControllerTest
  @Autowired
  constructor(
    private val userRepository: UserRepository,
    private val inquiryRepository: InquiryRepository,
  ) : BaseIntegrationTest() {
    private lateinit var testUser: User
    private lateinit var authentication: UsernamePasswordAuthenticationToken

    @BeforeEach
    fun setUp() =
      runTest {
        val savedUser =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.GOOGLE,
              providerId = "test-provider-123",
            ),
          )
        testUser = User.from(savedUser)

        authentication =
          UsernamePasswordAuthenticationToken(
            testUser,
            null,
            listOf(SimpleGrantedAuthority(Role.USER.name)),
          )
      }

    @Test
    fun `문의 생성 성공`() =
      runTest {
        // given
        val request =
          CreateInquiryRequest(
            inquiryType = InquiryType.BUG_REPORT,
            content = "앱이 자주 크래시됩니다.",
            contactEmail = "user@example.com",
          )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/inquiries")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<InquiryResponse>>()
          .value { response ->
            assertThat(response.data?.inquiryType).isEqualTo(InquiryType.BUG_REPORT)
            assertThat(response.data?.content).isEqualTo("앱이 자주 크래시됩니다.")
            assertThat(response.data?.contactEmail).isEqualTo("user@example.com")
            assertThat(response.data?.userId).isEqualTo(testUser.id)
            assertThat(response.data?.status).isEqualTo(InquiryStatus.PENDING)
            assertThat(response.data?.id).isNotNull
          }
      }

    @Test
    fun `연락처 이메일 없이 문의 생성 성공`() =
      runTest {
        // given
        val request =
          CreateInquiryRequest(
            inquiryType = InquiryType.GENERAL,
            content = "일반적인 문의사항입니다.",
          )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/inquiries")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<InquiryResponse>>()
          .value { response ->
            assertThat(response.data?.inquiryType).isEqualTo(InquiryType.GENERAL)
            assertThat(response.data?.content).isEqualTo("일반적인 문의사항입니다.")
            assertThat(response.data?.contactEmail).isNull()
            assertThat(response.data?.userId).isEqualTo(testUser.id)
            assertThat(response.data?.status).isEqualTo(InquiryStatus.PENDING)
          }
      }

    @Test
    fun `빈 내용으로 문의 생성 시 400 반환`() =
      runTest {
        // given
        val request =
          CreateInquiryRequest(
            inquiryType = InquiryType.BUG_REPORT,
            content = "",
          )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/inquiries")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isBadRequest
      }

    @Test
    fun `1000자 초과 내용으로 문의 생성 시 400 반환`() =
      runTest {
        // given
        val longContent = "a".repeat(1001)
        val request =
          CreateInquiryRequest(
            inquiryType = InquiryType.BUG_REPORT,
            content = longContent,
          )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/inquiries")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isBadRequest
      }

    @Test
    fun `잘못된 이메일 형식으로 문의 생성 시 400 반환`() =
      runTest {
        // given
        val request =
          CreateInquiryRequest(
            inquiryType = InquiryType.BUG_REPORT,
            content = "앱이 자주 크래시됩니다.",
            contactEmail = "invalid-email",
          )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/inquiries")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isBadRequest
      }

    @Test
    fun `인증되지 않은 사용자는 401 반환`() =
      runTest {
        // given
        val request =
          CreateInquiryRequest(
            inquiryType = InquiryType.BUG_REPORT,
            content = "앱이 자주 크래시됩니다.",
          )

        // when & then
        webTestClient
          .post()
          .uri("/api/v1/inquiries")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isUnauthorized
      }

    @Test
    fun `모든 문의 유형 테스트`() =
      runTest {
        val inquiryTypes = InquiryType.entries

        inquiryTypes.forEach { type ->
          // given
          val request =
            CreateInquiryRequest(
              inquiryType = type,
              content = "문의 내용: ${type.name}",
            )

          // when & then
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/inquiries")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<InquiryResponse>>()
            .value { response ->
              assertThat(response.data?.inquiryType).isEqualTo(type)
              assertThat(response.data?.content).isEqualTo("문의 내용: ${type.name}")
            }
        }
      }
  }
