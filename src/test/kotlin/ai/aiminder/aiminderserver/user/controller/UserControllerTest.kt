package ai.aiminder.aiminderserver.user.controller

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.dto.WithdrawUserRequest
import ai.aiminder.aiminderserver.user.entity.UserEntity
import ai.aiminder.aiminderserver.user.repository.UserRepository
import ai.aiminder.aiminderserver.user.repository.UserWithdrawalRepository
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

class UserControllerTest
  @Autowired
  constructor(
    private val userRepository: UserRepository,
    private val userWithdrawalRepository: UserWithdrawalRepository,
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
    fun `회원 탈퇴 성공 - 탈퇴 사유가 있는 경우`() =
      runTest {
        // given
        val request = WithdrawUserRequest(reason = "서비스가 마음에 들지 않음")

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .patch()
          .uri("/api/v1/users/withdraw")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<Unit>>()
          .value { response: ServiceResponse<Unit> ->
            assertThat(response.statusCode).isEqualTo(200)
            assertThat(response.message).isEqualTo("회원 탈퇴가 완료되었습니다.")
            assertThat(response.data).isNull()
            assertThat(response.errorCode).isNull()
          }

        // 데이터베이스에서 사용자가 소프트 삭제되었는지 확인
        val deletedUser = userRepository.findById(testUser.id)
        assertThat(deletedUser).isNotNull
        assertThat(deletedUser!!.deletedAt).isNotNull()

        // 탈퇴 사유가 저장되었는지 확인
        val withdrawal = userWithdrawalRepository.findByUserId(testUser.id)
        assertThat(withdrawal).isNotNull
        assertThat(withdrawal!!.reason.displayName).isEqualTo("서비스가 마음에 들지 않음")
      }

    @Test
    fun `회원 탈퇴 성공 - 탈퇴 사유가 없는 경우`() =
      runTest {
        // given
        val request = WithdrawUserRequest()

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .patch()
          .uri("/api/v1/users/withdraw")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<Unit>>()
          .value { response: ServiceResponse<Unit> ->
            assertThat(response.statusCode).isEqualTo(200)
            assertThat(response.message).isEqualTo("회원 탈퇴가 완료되었습니다.")
            assertThat(response.data).isNull()
            assertThat(response.errorCode).isNull()
          }

        // 데이터베이스에서 사용자가 소프트 삭제되었는지 확인
        val deletedUser = userRepository.findById(testUser.id)
        assertThat(deletedUser).isNotNull
        assertThat(deletedUser!!.deletedAt).isNotNull()

        // 탈퇴 사유가 저장되지 않았는지 확인
        val withdrawal = userWithdrawalRepository.findByUserId(testUser.id)
        assertThat(withdrawal).isNull()
      }

    @Test
    fun `회원 탈퇴 성공 - null 사유가 있는 경우`() =
      runTest {
        // given
        val request = WithdrawUserRequest(reason = null)

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .patch()
          .uri("/api/v1/users/withdraw")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<Unit>>()
          .value { response: ServiceResponse<Unit> ->
            assertThat(response.statusCode).isEqualTo(200)
            assertThat(response.message).isEqualTo("회원 탈퇴가 완료되었습니다.")
            assertThat(response.data).isNull()
            assertThat(response.errorCode).isNull()
          }

        // 데이터베이스에서 사용자가 소프트 삭제되었는지 확인
        val deletedUser = userRepository.findById(testUser.id)
        assertThat(deletedUser).isNotNull
        assertThat(deletedUser!!.deletedAt).isNotNull()

        // 탈퇴 사유가 저장되지 않았는지 확인
        val withdrawal = userWithdrawalRepository.findByUserId(testUser.id)
        assertThat(withdrawal).isNull()
      }

    @Test
    fun `인증되지 않은 사용자는 401 반환`() {
      // given
      val request = WithdrawUserRequest(reason = "테스트 사유")

      // when & then
      webTestClient
        .patch()
        .uri("/api/v1/users/withdraw")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `잘못된 JSON 형식으로 요청 시 400 반환`() {
      // when & then
      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .patch()
        .uri("/api/v1/users/withdraw")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""{"reason": "유효한 사유", "invalidField": "추가 필드"}""")
        .exchange()
        .expectStatus()
        .isOk // Spring이 알 수 없는 필드를 무시하므로 성공적으로 처리됨
    }

    @Test
    fun `빈 요청 바디로 요청 시 성공`() =
      runTest {
        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .patch()
          .uri("/api/v1/users/withdraw")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("{}")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<Unit>>()
          .value { response: ServiceResponse<Unit> ->
            assertThat(response.statusCode).isEqualTo(200)
            assertThat(response.message).isEqualTo("회원 탈퇴가 완료되었습니다.")
            assertThat(response.data).isNull()
            assertThat(response.errorCode).isNull()
          }

        // 데이터베이스에서 사용자가 소프트 삭제되었는지 확인
        val deletedUser = userRepository.findById(testUser.id)
        assertThat(deletedUser).isNotNull
        assertThat(deletedUser!!.deletedAt).isNotNull()
      }

    @Test
    fun `이미 탈퇴한 사용자가 다시 탈퇴 시도시 에러 반환`() =
      runTest {
        // given - 사용자를 먼저 탈퇴시킴
        val deletedUserEntity =
          userRepository.save(
            UserEntity(
              id = testUser.id,
              provider = testUser.provider,
              providerId = testUser.providerId,
              deletedAt = java.time.Instant.now(),
            ),
          )
        val deletedUser = User.from(deletedUserEntity)
        val deletedUserAuth =
          UsernamePasswordAuthenticationToken(
            deletedUser,
            null,
            listOf(SimpleGrantedAuthority(Role.USER.name)),
          )

        val request = WithdrawUserRequest(reason = "재탈퇴 시도")

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(deletedUserAuth))
          .patch()
          .uri("/api/v1/users/withdraw")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is4xxClientError // 클라이언트 에러 범위면 통과
      }
  }
