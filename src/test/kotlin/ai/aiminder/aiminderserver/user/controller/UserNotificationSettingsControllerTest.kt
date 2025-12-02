package ai.aiminder.aiminderserver.user.controller

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.dto.GetUserNotificationSettingsResponse
import ai.aiminder.aiminderserver.user.dto.UpdateAiFeedbackEnabledRequest
import ai.aiminder.aiminderserver.user.dto.UpdateAiFeedbackNotificationTimeRequest
import ai.aiminder.aiminderserver.user.entity.UserEntity
import ai.aiminder.aiminderserver.user.entity.UserNotificationSettingsEntity
import ai.aiminder.aiminderserver.user.repository.UserNotificationSettingsRepository
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
import java.time.LocalTime

class UserNotificationSettingsControllerTest
  @Autowired
  constructor(
    private val userRepository: UserRepository,
    private val userNotificationSettingsRepository: UserNotificationSettingsRepository,
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
    fun `기본 알림 설정 조회 성공`() =
      runTest {
        // given - 기본 설정이 생성되어야 함

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/user/notification-settings")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<GetUserNotificationSettingsResponse>>()
          .value { response ->
            assertThat(response.statusCode).isEqualTo(200)
            assertThat(response.data).isNotNull
            assertThat(response.data!!.aiFeedbackEnabled).isTrue()
            assertThat(response.data!!.aiFeedbackNotificationTime).isEqualTo(LocalTime.of(9, 0))
            assertThat(response.errorCode).isNull()
          }
      }

    @Test
    fun `기존 설정이 있는 경우 알림 설정 조회 성공`() =
      runTest {
        // given - 기존 설정 생성
        userNotificationSettingsRepository.save(
          UserNotificationSettingsEntity(
            id = testUser.id,
            aiFeedbackEnabled = false,
            aiFeedbackNotificationTime = LocalTime.of(18, 30),
          ),
        )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/user/notification-settings")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<GetUserNotificationSettingsResponse>>()
          .value { response ->
            assertThat(response.statusCode).isEqualTo(200)
            assertThat(response.data).isNotNull
            assertThat(response.data!!.aiFeedbackEnabled).isFalse()
            assertThat(response.data!!.aiFeedbackNotificationTime).isEqualTo(LocalTime.of(18, 30))
            assertThat(response.errorCode).isNull()
          }
      }

    @Test
    fun `인증되지 않은 사용자는 401 반환 - getNotificationSettings`() {
      // when & then
      webTestClient
        .get()
        .uri("/api/v1/user/notification-settings")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `AI 피드백 활성화 설정 업데이트 성공`() =
      runTest {
        // given
        val request = UpdateAiFeedbackEnabledRequest(aiFeedbackEnabled = false)

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .patch()
          .uri("/api/v1/user/notification-settings/ai-feedback-enabled")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<GetUserNotificationSettingsResponse>>()
          .value { response ->
            assertThat(response.statusCode).isEqualTo(200)
            assertThat(response.data).isNotNull
            assertThat(response.data!!.aiFeedbackEnabled).isFalse()
            assertThat(response.data!!.aiFeedbackNotificationTime).isEqualTo(LocalTime.of(9, 0))
            assertThat(response.errorCode).isNull()
          }

        // 데이터베이스에서 실제로 변경되었는지 확인
        val savedSettings = userNotificationSettingsRepository.findById(testUser.id)
        assertThat(savedSettings).isNotNull
        assertThat(savedSettings!!.aiFeedbackEnabled).isFalse()
      }

    @Test
    fun `기존 설정이 있는 경우 AI 피드백 활성화 설정 업데이트 성공`() =
      runTest {
        // given - 기존 설정
        userNotificationSettingsRepository.save(
          UserNotificationSettingsEntity(
            id = testUser.id,
            aiFeedbackEnabled = true,
            aiFeedbackNotificationTime = LocalTime.of(15, 45),
          ),
        )
        val request = UpdateAiFeedbackEnabledRequest(aiFeedbackEnabled = false)

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .patch()
          .uri("/api/v1/user/notification-settings/ai-feedback-enabled")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<GetUserNotificationSettingsResponse>>()
          .value { response ->
            assertThat(response.statusCode).isEqualTo(200)
            assertThat(response.data).isNotNull
            assertThat(response.data!!.aiFeedbackEnabled).isFalse()
            assertThat(response.data!!.aiFeedbackNotificationTime).isEqualTo(LocalTime.of(15, 45))
            assertThat(response.errorCode).isNull()
          }

        // 데이터베이스에서 변경사항 확인
        val savedSettings = userNotificationSettingsRepository.findById(testUser.id)
        assertThat(savedSettings).isNotNull
        assertThat(savedSettings!!.aiFeedbackEnabled).isFalse()
        assertThat(savedSettings.aiFeedbackNotificationTime).isEqualTo(LocalTime.of(15, 45))
      }

    @Test
    fun `인증되지 않은 사용자는 401 반환 - updateAiFeedbackEnabled`() {
      // given
      val request = UpdateAiFeedbackEnabledRequest(aiFeedbackEnabled = false)

      // when & then
      webTestClient
        .patch()
        .uri("/api/v1/user/notification-settings/ai-feedback-enabled")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `AI 피드백 알림 시간 업데이트 성공`() =
      runTest {
        // given
        val newTime = LocalTime.of(20, 30)
        val request = UpdateAiFeedbackNotificationTimeRequest(aiFeedbackNotificationTime = newTime)

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .patch()
          .uri("/api/v1/user/notification-settings/ai-feedback-notification-time")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<GetUserNotificationSettingsResponse>>()
          .value { response ->
            assertThat(response.statusCode).isEqualTo(200)
            assertThat(response.data).isNotNull
            assertThat(response.data!!.aiFeedbackEnabled).isTrue()
            assertThat(response.data!!.aiFeedbackNotificationTime).isEqualTo(newTime)
            assertThat(response.errorCode).isNull()
          }

        // 데이터베이스에서 실제로 변경되었는지 확인
        val savedSettings = userNotificationSettingsRepository.findById(testUser.id)
        assertThat(savedSettings).isNotNull
        assertThat(savedSettings!!.aiFeedbackNotificationTime).isEqualTo(newTime)
      }

    @Test
    fun `기존 설정이 있는 경우 AI 피드백 알림 시간 업데이트 성공`() =
      runTest {
        // given - 기존 설정
        userNotificationSettingsRepository.save(
          UserNotificationSettingsEntity(
            id = testUser.id,
            aiFeedbackEnabled = false,
            aiFeedbackNotificationTime = LocalTime.of(10, 0),
          ),
        )
        val newTime = LocalTime.of(22, 15)
        val request = UpdateAiFeedbackNotificationTimeRequest(aiFeedbackNotificationTime = newTime)

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .patch()
          .uri("/api/v1/user/notification-settings/ai-feedback-notification-time")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<GetUserNotificationSettingsResponse>>()
          .value { response ->
            assertThat(response.statusCode).isEqualTo(200)
            assertThat(response.data).isNotNull
            assertThat(response.data!!.aiFeedbackEnabled).isFalse()
            assertThat(response.data!!.aiFeedbackNotificationTime).isEqualTo(newTime)
            assertThat(response.errorCode).isNull()
          }

        // 데이터베이스에서 변경사항 확인
        val savedSettings = userNotificationSettingsRepository.findById(testUser.id)
        assertThat(savedSettings).isNotNull
        assertThat(savedSettings!!.aiFeedbackEnabled).isFalse()
        assertThat(savedSettings.aiFeedbackNotificationTime).isEqualTo(newTime)
      }

    @Test
    fun `인증되지 않은 사용자는 401 반환 - updateAiFeedbackNotificationTime`() {
      // given
      val request =
        UpdateAiFeedbackNotificationTimeRequest(
          aiFeedbackNotificationTime = LocalTime.of(20, 30),
        )

      // when & then
      webTestClient
        .patch()
        .uri("/api/v1/user/notification-settings/ai-feedback-notification-time")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `잘못된 시간 형식으로 요청 시 400 반환`() {
      // when & then
      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .patch()
        .uri("/api/v1/user/notification-settings/ai-feedback-notification-time")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""{"aiFeedbackNotificationTime": "25:61"}""")
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `잘못된 타입으로 요청 시 400 반환 - updateAiFeedbackEnabled`() {
      // when & then
      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .patch()
        .uri("/api/v1/user/notification-settings/ai-feedback-enabled")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""{"aiFeedbackEnabled": "invalid_boolean"}""")
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `빈 요청으로 요청 시 400 반환 - updateAiFeedbackNotificationTime`() {
      // when & then
      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .patch()
        .uri("/api/v1/user/notification-settings/ai-feedback-notification-time")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isBadRequest
    }
  }
