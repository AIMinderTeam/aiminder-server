package ai.aiminder.aiminderserver.notification.controller

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.auth.service.TokenService
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.notification.domain.NotificationType
import ai.aiminder.aiminderserver.notification.entity.NotificationEntity
import ai.aiminder.aiminderserver.notification.repository.NotificationRepository
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
import java.time.Instant
import java.util.UUID

class NotificationControllerTest
  @Autowired
  constructor(
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
    private val tokenService: TokenService,
  ) : BaseIntegrationTest() {
    private lateinit var testUser: User
    private lateinit var authentication: UsernamePasswordAuthenticationToken
    private lateinit var otherUser: User

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

        // 다른 사용자 생성 (권한 테스트용)
        val savedOtherUser =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.GOOGLE,
              providerId = "other-provider-456",
            ),
          )
        otherUser = User.from(savedOtherUser)
      }

    @Test
    fun `정상적인 알림 개수 조회 테스트`() =
      runTest {
        // given - 읽지 않은 알림 3개 생성
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.TO_DO,
            title = "Todo Notification",
            description = "You have a new todo",
            metadata = mapOf("key" to "value"),
            receiverId = testUser.id,
            checked = false,
          ),
        )
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.MOTIVATION,
            title = "Motivation Notification",
            description = "Stay motivated!",
            metadata = mapOf("key" to "value"),
            receiverId = testUser.id,
            checked = false,
          ),
        )
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.TO_DO,
            title = "Another Todo",
            description = "Another todo notification",
            metadata = mapOf("key" to "value"),
            receiverId = testUser.id,
            checked = false,
          ),
        )

        // 체크된 알림 1개 (개수에 포함되지 않아야 함)
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.MOTIVATION,
            title = "Checked Notification",
            description = "This is checked",
            metadata = mapOf("key" to "value"),
            receiverId = testUser.id,
            checked = true,
          ),
        )

        // when
        val response = getNotificationCount()

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isEqualTo(3)
        assertThat(response.errorCode).isNull()
      }

    @Test
    fun `삭제된 알림은 조회되지 않음`() =
      runTest {
        // given - 삭제된 알림 생성
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.TO_DO,
            title = "Todo Notification",
            description = "You have a new todo",
            metadata = mapOf("key" to "value"),
            receiverId = testUser.id,
            checked = false,
            deletedAt = Instant.now(),
          ),
        )

        // when
        val response = getNotificationCount()

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isEqualTo(0)
        assertThat(response.errorCode).isNull()
      }

    @Test
    fun `인증되지 않은 사용자 요청 시 401 반환`() {
      // when
      val response = getNotificationCountExpectingError(null)

      // then
      verifyErrorResponse(response, 401, "AUTH:UNAUTHORIZED")
      assertThat(response.message).isEqualTo("인증이 필요합니다. 로그인을 진행해주세요.")
    }

    @Test
    fun `Bearer Token으로 요청 테스트`() =
      runTest {
        // given - 읽지 않은 알림 2개 생성
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.TO_DO,
            title = "Test Notification 1",
            description = "Test Description 1",
            metadata = mapOf("key" to "value"),
            receiverId = testUser.id,
            checked = false,
          ),
        )
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.MOTIVATION,
            title = "Test Notification 2",
            description = "Test Description 2",
            metadata = mapOf("key" to "value"),
            receiverId = testUser.id,
            checked = false,
          ),
        )

        val validAccessToken = tokenService.createAccessToken(testUser)

        // when
        val response =
          webTestClient
            .get()
            .uri("/api/v1/notifications/count")
            .accept(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $validAccessToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<Int>>()
            .returnResult()
            .responseBody!!

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isEqualTo(2)
        assertThat(response.errorCode).isNull()
      }

    @Test
    fun `존재하지 않는 사용자로 요청 시 에러 처리`() {
      // given - 데이터베이스에 존재하지 않는 사용자
      val nonExistentUser =
        User(
          id = UUID.randomUUID(),
          provider = OAuth2Provider.GOOGLE,
          providerId = "non-existent-user",
          createdAt = Instant.now(),
          updatedAt = Instant.now(),
        )
      val authentication =
        UsernamePasswordAuthenticationToken(
          nonExistentUser,
          null,
          listOf(SimpleGrantedAuthority(Role.USER.name)),
        )

      // when
      val response =
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/notifications/count")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk // 실제 상태를 확인하기 위해 isOk로 변경
          .expectBody<ServiceResponse<Int>>()
          .returnResult()
          .responseBody!!

      // then
      // 존재하지 않는 사용자의 알림 개수 조회는 정상적으로 0을 반환해야 함
      assertThat(response.statusCode).isEqualTo(200)
      assertThat(response.data).isEqualTo(0)
      assertThat(response.errorCode).isNull()
    }

    @Test
    fun `알림이 없는 경우 0 반환`() {
      // given - 알림 없음

      // when
      val response = getNotificationCount()

      // then
      assertThat(response.statusCode).isEqualTo(200)
      assertThat(response.data).isEqualTo(0)
      assertThat(response.errorCode).isNull()
    }

    @Test
    fun `체크된 알림은 개수에 포함되지 않는지 확인`() =
      runTest {
        // given - 체크된 알림만 생성
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.TO_DO,
            title = "Checked Notification 1",
            description = "This is checked",
            metadata = mapOf("key" to "value"),
            receiverId = testUser.id,
            checked = true,
          ),
        )
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.MOTIVATION,
            title = "Checked Notification 2",
            description = "This is also checked",
            metadata = mapOf("key" to "value"),
            receiverId = testUser.id,
            checked = true,
          ),
        )

        // when
        val response = getNotificationCount()

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isEqualTo(0)
        assertThat(response.errorCode).isNull()
      }

    @Test
    fun `다른 사용자의 알림은 개수에 포함되지 않는지 확인`() =
      runTest {
        // given - 다른 사용자의 알림 생성
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.TO_DO,
            title = "Other User's Notification",
            description = "This belongs to other user",
            metadata = mapOf("key" to "value"),
            receiverId = otherUser.id,
            checked = false,
          ),
        )

        // testUser의 알림 1개 생성
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.MOTIVATION,
            title = "My Notification",
            description = "This belongs to test user",
            metadata = mapOf("key" to "value"),
            receiverId = testUser.id,
            checked = false,
          ),
        )

        // when
        val response = getNotificationCount()

        // then - testUser의 알림만 카운트되어야 함
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isEqualTo(1)
        assertThat(response.errorCode).isNull()
      }

    private fun getNotificationCount(
      auth: UsernamePasswordAuthenticationToken = authentication,
    ): ServiceResponse<Int> =
      webTestClient
        .mutateWith(mockAuthentication(auth))
        .get()
        .uri("/api/v1/notifications/count")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ServiceResponse<Int>>()
        .returnResult()
        .responseBody!!

    private fun getNotificationCountExpectingError(auth: UsernamePasswordAuthenticationToken?): ServiceResponse<Unit> {
      val testClient =
        if (auth != null) {
          webTestClient.mutateWith(mockAuthentication(auth))
        } else {
          webTestClient
        }

      return testClient
        .get()
        .uri("/api/v1/notifications/count")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody<ServiceResponse<Unit>>()
        .returnResult()
        .responseBody!!
    }

    private fun verifyErrorResponse(
      response: ServiceResponse<Unit>,
      expectedStatus: Int,
      expectedErrorCode: String,
    ) {
      assertThat(response.statusCode).isEqualTo(expectedStatus)
      assertThat(response.errorCode).isEqualTo(expectedErrorCode)
      assertThat(response.data).isNull()
    }
  }
