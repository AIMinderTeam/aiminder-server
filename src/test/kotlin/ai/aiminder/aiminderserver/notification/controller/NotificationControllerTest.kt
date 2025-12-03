package ai.aiminder.aiminderserver.notification.controller

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.auth.service.TokenService
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.notification.domain.Notification
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
            type = NotificationType.ASSISTANT_FEEDBACK,
            title = "Todo Notification",
            description = "You have a new todo",
            metadata = mapOf("key" to "value"),
            receiverId = testUser.id,
            checked = false,
          ),
        )
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.ASSISTANT_FEEDBACK,
            title = "Motivation Notification",
            description = "Stay motivated!",
            metadata = mapOf("key" to "value"),
            receiverId = testUser.id,
            checked = false,
          ),
        )
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.ASSISTANT_FEEDBACK,
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
            type = NotificationType.ASSISTANT_FEEDBACK,
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
            type = NotificationType.ASSISTANT_FEEDBACK,
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
            type = NotificationType.ASSISTANT_FEEDBACK,
            title = "Test Notification 1",
            description = "Test Description 1",
            metadata = mapOf("key" to "value"),
            receiverId = testUser.id,
            checked = false,
          ),
        )
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.ASSISTANT_FEEDBACK,
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
            type = NotificationType.ASSISTANT_FEEDBACK,
            title = "Checked Notification 1",
            description = "This is checked",
            metadata = mapOf("key" to "value"),
            receiverId = testUser.id,
            checked = true,
          ),
        )
        notificationRepository.save(
          NotificationEntity(
            type = NotificationType.ASSISTANT_FEEDBACK,
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
            type = NotificationType.ASSISTANT_FEEDBACK,
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
            type = NotificationType.ASSISTANT_FEEDBACK,
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

    @Test
    fun `정상적인 알림 목록 조회 테스트`() =
      runTest {
        // given - 읽지 않은 알림 3개 생성
        val notification1 =
          createTestNotification(
            testUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Todo 1",
            "Description 1",
          )
        val notification2 =
          createTestNotification(
            testUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Motivation 1",
            "Description 2",
          )
        val notification3 =
          createTestNotification(
            testUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Todo 2",
            "Description 3",
          )

        // when
        val response = getNotifications("/api/v1/notifications?page=0&size=10")

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).hasSize(3)
        assertThat(response.errorCode).isNull()

        // 페이지네이션 정보 확인
        assertThat(response.pageable?.page).isEqualTo(0)
        assertThat(response.pageable?.count).isEqualTo(3)
        assertThat(response.pageable?.totalElements).isEqualTo(3)
        assertThat(response.pageable?.totalPages).isEqualTo(1)

        // 알림 데이터 일치성 확인 (생성일 기준 내림차순 정렬)
        val notifications = response.data!!
        verifyNotificationConsistency(notifications[0], notification3)
        verifyNotificationConsistency(notifications[1], notification2)
        verifyNotificationConsistency(notifications[2], notification1)
      }

    @Test
    fun `빈 알림 목록 조회 테스트`() {
      // given - 알림 없음

      // when
      val response = getNotifications("/api/v1/notifications?page=0&size=10")

      // then
      assertThat(response.statusCode).isEqualTo(200)
      assertThat(response.data).isEmpty()
      assertThat(response.errorCode).isNull()

      // 페이지네이션 정보 확인
      assertThat(response.pageable?.page).isEqualTo(0)
      assertThat(response.pageable?.count).isEqualTo(0)
      assertThat(response.pageable?.totalElements).isEqualTo(0)
      assertThat(response.pageable?.totalPages).isEqualTo(0)
    }

    @Test
    fun `삭제된 알림은 조회되지 않는지 확인`() =
      runTest {
        // given - 정상 알림 1개, 삭제된 알림 1개
        val normalNotification = createTestNotification(testUser)
        createTestNotification(
          testUser,
          deletedAt = Instant.now(),
        ) // 삭제된 알림 (조회되지 않아야 함)

        // when
        val response = getNotifications("/api/v1/notifications?page=0&size=10")

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).hasSize(1)
        assertThat(response.pageable?.totalElements).isEqualTo(1)

        // 조회된 알림이 삭제되지 않은 것인지 확인
        val notification = response.data!!.first()
        verifyNotificationConsistency(notification, normalNotification)
        assertThat(notification.deletedAt).isNull()
      }

    @Test
    fun `다른 사용자의 알림은 조회되지 않는지 확인`() =
      runTest {
        // given - testUser 알림 1개, otherUser 알림 1개
        val myNotification = createTestNotification(testUser)
        createTestNotification(otherUser) // 다른 사용자 알림 (조회되지 않아야 함)

        // when
        val response = getNotifications("/api/v1/notifications?page=0&size=10")

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).hasSize(1)
        assertThat(response.pageable?.totalElements).isEqualTo(1)

        // 조회된 알림이 내 알림인지 확인
        val notification = response.data!!.first()
        verifyNotificationConsistency(notification, myNotification)
        assertThat(notification.receiverId).isEqualTo(testUser.id)
      }

    @Test
    fun `생성일 기준 내림차순 정렬 확인`() =
      runTest {
        // given - 시간 간격을 두고 3개 알림 생성
        createTestNotification(
          testUser,
          title = "First Notification",
        )

        // 약간의 시간 간격
        Thread.sleep(10)

        createTestNotification(
          testUser,
          title = "Second Notification",
        )

        Thread.sleep(10)

        val thirdNotification =
          createTestNotification(
            testUser,
            title = "Third Notification",
          )

        // when
        val response = getNotifications("/api/v1/notifications?page=0&size=10")

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).hasSize(3)

        val notifications = response.data!!
        // 생성일 기준 내림차순으로 정렬되어야 함 (최신순)
        assertThat(notifications[0].title).isEqualTo("Third Notification")
        assertThat(notifications[1].title).isEqualTo("Second Notification")
        assertThat(notifications[2].title).isEqualTo("First Notification")

        // 생성일 확인
        assertThat(notifications[0].createdAt).isAfterOrEqualTo(notifications[1].createdAt)
        assertThat(notifications[1].createdAt).isAfterOrEqualTo(notifications[2].createdAt)
      }

    @Test
    fun `인증되지 않은 사용자 요청 시 401 반환 - getNotifications`() {
      // when
      val response = getNotificationsExpectingError("/api/v1/notifications?page=0&size=10", null)

      // then
      verifyErrorResponse(response, 401, "AUTH:UNAUTHORIZED")
    }

    @Test
    fun `Bearer Token으로 정상 요청 테스트 - getNotifications`() =
      runTest {
        // given - 알림 생성
        createTestNotification(testUser, title = "Bearer Token Test")
        val validAccessToken = tokenService.createAccessToken(testUser)

        // when
        val response =
          webTestClient
            .get()
            .uri("/api/v1/notifications?page=0&size=10")
            .accept(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $validAccessToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<List<Notification>>>()
            .returnResult()
            .responseBody!!

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).hasSize(1)
        assertThat(response.data!!.first().title).isEqualTo("Bearer Token Test")
        assertThat(response.errorCode).isNull()
      }

    @Test
    fun `첫 번째 페이지 조회 테스트`() =
      runTest {
        // given - 5개 알림 생성
        repeat(5) { index ->
          createTestNotification(testUser, title = "Notification ${index + 1}")
        }

        // when - 첫 번째 페이지, 페이지 크기 3
        val response = getNotifications("/api/v1/notifications?page=0&size=3")

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).hasSize(3)

        assertThat(response.pageable?.page).isEqualTo(0)
        assertThat(response.pageable?.count).isEqualTo(3)
        assertThat(response.pageable?.totalElements).isEqualTo(5)
        assertThat(response.pageable?.totalPages).isEqualTo(2)
      }

    @Test
    fun `두 번째 페이지 조회 테스트`() =
      runTest {
        // given - 5개 알림 생성
        repeat(5) { index ->
          createTestNotification(testUser, title = "Notification ${index + 1}")
        }

        // when - 두 번째 페이지, 페이지 크기 3
        val response = getNotifications("/api/v1/notifications?page=1&size=3")

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).hasSize(2) // 나머지 2개

        assertThat(response.pageable?.page).isEqualTo(1)
        assertThat(response.pageable?.count).isEqualTo(2)
        assertThat(response.pageable?.totalElements).isEqualTo(5)
        assertThat(response.pageable?.totalPages).isEqualTo(2)
      }

    @Test
    fun `페이지 크기 변경 테스트`() =
      runTest {
        // given - 10개 알림 생성
        repeat(10) { index ->
          createTestNotification(testUser, title = "Notification ${index + 1}")
        }

        // when - 페이지 크기 5로 설정
        val response = getNotifications("/api/v1/notifications?page=0&size=5")

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).hasSize(5)

        assertThat(response.pageable?.page).isEqualTo(0)
        assertThat(response.pageable?.count).isEqualTo(5)
        assertThat(response.pageable?.totalElements).isEqualTo(10)
        assertThat(response.pageable?.totalPages).isEqualTo(2)
      }

    @Test
    fun `페이지 번호 초과 시 빈 결과 반환`() =
      runTest {
        // given - 3개 알림 생성
        repeat(3) { index ->
          createTestNotification(testUser, title = "Notification ${index + 1}")
        }

        // when - 존재하지 않는 페이지 조회
        val response = getNotifications("/api/v1/notifications?page=5&size=3")

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isEmpty()

        assertThat(response.pageable?.page).isEqualTo(5)
        assertThat(response.pageable?.count).isEqualTo(0)
        assertThat(response.pageable?.totalElements).isEqualTo(3)
        assertThat(response.pageable?.totalPages).isEqualTo(1)
      }

    @Test
    fun `존재하지 않는 사용자로 요청 시 빈 결과 반환 - getNotifications`() =
      runTest {
        // given - 존재하지 않는 사용자
        val nonExistentUser =
          User(
            id = UUID.randomUUID(),
            provider = OAuth2Provider.GOOGLE,
            providerId = "non-existent-user",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
          )
        val nonExistentAuth =
          UsernamePasswordAuthenticationToken(
            nonExistentUser,
            null,
            listOf(SimpleGrantedAuthority(Role.USER.name)),
          )

        // testUser의 알림 생성 (다른 사용자 알림이므로 조회되지 않아야 함)
        createTestNotification(testUser)

        // when
        val response = getNotifications("/api/v1/notifications?page=0&size=10", nonExistentAuth)

        // then - 존재하지 않는 사용자의 경우 빈 결과 반환
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isEmpty()
        assertThat(response.pageable?.totalElements).isEqualTo(0)
        assertThat(response.errorCode).isNull()
      }

    private fun getNotificationCount(auth: UsernamePasswordAuthenticationToken = authentication): ServiceResponse<Int> =
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

    private fun getNotifications(
      uri: String,
      auth: UsernamePasswordAuthenticationToken = authentication,
    ): ServiceResponse<List<Notification>> =
      webTestClient
        .mutateWith(mockAuthentication(auth))
        .get()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ServiceResponse<List<Notification>>>()
        .returnResult()
        .responseBody!!

    private fun getNotificationsExpectingError(
      uri: String,
      auth: UsernamePasswordAuthenticationToken?,
    ): ServiceResponse<Unit> {
      val testClient =
        if (auth != null) {
          webTestClient.mutateWith(mockAuthentication(auth))
        } else {
          webTestClient
        }

      return testClient
        .get()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody<ServiceResponse<Unit>>()
        .returnResult()
        .responseBody!!
    }

    private suspend fun createTestNotification(
      user: User,
      type: NotificationType = NotificationType.ASSISTANT_FEEDBACK,
      title: String = "Test Notification",
      description: String = "Test Description",
      checked: Boolean = false,
      deletedAt: Instant? = null,
    ): NotificationEntity =
      notificationRepository.save(
        NotificationEntity(
          type = type,
          title = title,
          description = description,
          metadata = mapOf("key" to "value"),
          receiverId = user.id,
          checked = checked,
          deletedAt = deletedAt,
        ),
      )

    @Test
    fun `정상적인 알림 확인 테스트 - checkNotification`() =
      runTest {
        // given - 미확인 알림 생성
        val notificationEntity =
          createTestNotification(
            testUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Test Todo",
            "Test Description",
            checked = false,
          )

        // when
        val response = checkNotification(notificationEntity.id!!)

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isNotNull
        assertThat(response.data!!.id).isEqualTo(notificationEntity.id)
        assertThat(response.data!!.checked).isTrue()
        assertThat(response.errorCode).isNull()

        // 데이터베이스에서 실제로 상태가 변경되었는지 확인
        val updatedEntity = notificationRepository.findById(notificationEntity.id!!)
        assertThat(updatedEntity).isNotNull
        assertThat(updatedEntity!!.checked).isTrue()
      }

    @Test
    fun `이미 확인된 알림 재확인 테스트`() =
      runTest {
        // given - 이미 확인된 알림 생성
        val notificationEntity =
          createTestNotification(
            testUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Already Checked",
            "Already checked notification",
            checked = true,
          )

        // when
        val response = checkNotification(notificationEntity.id!!)

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isNotNull
        assertThat(response.data!!.id).isEqualTo(notificationEntity.id)
        assertThat(response.data!!.checked).isTrue()
        assertThat(response.errorCode).isNull()
      }

    @Test
    fun `존재하지 않는 알림 확인 시 에러 테스트`() {
      // given - 존재하지 않는 알림 ID
      val nonExistentId = UUID.randomUUID()

      // when
      val response = checkNotificationExpectingError(nonExistentId)

      // then
      verifyErrorResponse(response, 404, "NOTIFICATION:NOTFOUND")
    }

    @Test
    fun `다른 사용자의 알림 확인 시 권한 에러 테스트`() =
      runTest {
        // given - 다른 사용자의 알림 생성
        val otherUserNotification =
          createTestNotification(
            otherUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Other User's Notification",
            "This belongs to other user",
            checked = false,
          )

        // when - testUser가 다른 사용자의 알림 확인 시도
        val response = checkNotificationExpectingError(otherUserNotification.id!!)

        // then
        verifyErrorResponse(response, 404, "NOTIFICATION:NOTFOUND")

        // 다른 사용자의 알림 상태는 변경되지 않았는지 확인
        val unchangedEntity = notificationRepository.findById(otherUserNotification.id!!)
        assertThat(unchangedEntity).isNotNull
        assertThat(unchangedEntity!!.checked).isFalse()
      }

    @Test
    fun `인증되지 않은 사용자 요청 시 401 에러 - checkNotification`() =
      runTest {
        // given - 테스트 알림 생성
        val notificationEntity = createTestNotification(testUser)

        // when
        val response = checkNotificationExpectingError(notificationEntity.id!!, null)

        // then
        verifyErrorResponse(response, 401, "AUTH:UNAUTHORIZED")
      }

    @Test
    fun `Bearer Token으로 알림 확인 테스트`() =
      runTest {
        // given - 미확인 알림 생성
        val notificationEntity =
          createTestNotification(
            testUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Bearer Token Test",
            "Test with bearer token",
            checked = false,
          )
        val validAccessToken = tokenService.createAccessToken(testUser)

        // when
        val response =
          webTestClient
            .patch()
            .uri("/api/v1/notifications/${notificationEntity.id}/check")
            .accept(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $validAccessToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<Notification>>()
            .returnResult()
            .responseBody!!

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isNotNull
        assertThat(response.data!!.id).isEqualTo(notificationEntity.id)
        assertThat(response.data!!.checked).isTrue()
        assertThat(response.errorCode).isNull()
      }

    @Test
    fun `삭제된 알림 확인 시 에러 테스트`() =
      runTest {
        // given - 삭제된 알림 생성
        val deletedNotification =
          createTestNotification(
            testUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Deleted Notification",
            "This is deleted",
            checked = false,
            deletedAt = Instant.now(),
          )

        // when
        val response = checkNotificationExpectingError(deletedNotification.id!!)

        // then
        verifyErrorResponse(response, 404, "NOTIFICATION:NOTFOUND")
      }

    private fun checkNotification(
      notificationId: UUID,
      auth: UsernamePasswordAuthenticationToken = authentication,
    ): ServiceResponse<Notification> =
      webTestClient
        .mutateWith(mockAuthentication(auth))
        .patch()
        .uri("/api/v1/notifications/$notificationId/check")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ServiceResponse<Notification>>()
        .returnResult()
        .responseBody!!

    private fun checkNotificationExpectingError(
      notificationId: UUID,
      auth: UsernamePasswordAuthenticationToken? = authentication,
    ): ServiceResponse<Unit> {
      val testClient =
        if (auth != null) {
          webTestClient.mutateWith(mockAuthentication(auth))
        } else {
          webTestClient
        }

      return testClient
        .patch()
        .uri("/api/v1/notifications/$notificationId/check")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody<ServiceResponse<Unit>>()
        .returnResult()
        .responseBody!!
    }

    @Test
    fun `정상적인 모든 알림 일괄 확인 테스트 - checkNotifications`() =
      runTest {
        // given - 미확인 알림 3개, 확인된 알림 1개 생성
        val uncheckedNotification1 =
          createTestNotification(
            testUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Unchecked 1",
            "Description 1",
            checked = false,
          )
        val uncheckedNotification2 =
          createTestNotification(
            testUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Unchecked 2",
            "Description 2",
            checked = false,
          )
        val uncheckedNotification3 =
          createTestNotification(
            testUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Unchecked 3",
            "Description 3",
            checked = false,
          )
        val checkedNotification =
          createTestNotification(
            testUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Already Checked",
            "Already checked",
            checked = true,
          )

        // when
        val response = checkNotifications()

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).hasSize(3) // 미확인 알림 3개만 반환
        assertThat(response.errorCode).isNull()

        // 반환된 알림들이 모두 checked=true인지 확인
        response.data!!.forEach { notification ->
          assertThat(notification.checked).isTrue()
          assertThat(notification.receiverId).isEqualTo(testUser.id)
        }

        // 데이터베이스에서 실제로 상태가 변경되었는지 확인
        val updatedUnchecked1 = notificationRepository.findById(uncheckedNotification1.id!!)
        val updatedUnchecked2 = notificationRepository.findById(uncheckedNotification2.id!!)
        val updatedUnchecked3 = notificationRepository.findById(uncheckedNotification3.id!!)
        val unchangedChecked = notificationRepository.findById(checkedNotification.id!!)

        assertThat(updatedUnchecked1!!.checked).isTrue()
        assertThat(updatedUnchecked2!!.checked).isTrue()
        assertThat(updatedUnchecked3!!.checked).isTrue()
        assertThat(unchangedChecked!!.checked).isTrue() // 이미 확인된 상태 유지

        // 반환된 알림 ID들이 미확인 알림들과 일치하는지 확인
        val returnedIds = response.data!!.map { it.id }.toSet()
        val expectedIds = setOf(uncheckedNotification1.id, uncheckedNotification2.id, uncheckedNotification3.id)
        assertThat(returnedIds).isEqualTo(expectedIds)
      }

    @Test
    fun `확인할 알림이 없는 경우 빈 리스트 반환 - checkNotifications`() =
      runTest {
        // given - 이미 확인된 알림만 존재
        createTestNotification(
          testUser,
          NotificationType.ASSISTANT_FEEDBACK,
          "Already Checked 1",
          "Description 1",
          checked = true,
        )
        createTestNotification(
          testUser,
          NotificationType.ASSISTANT_FEEDBACK,
          "Already Checked 2",
          "Description 2",
          checked = true,
        )

        // when
        val response = checkNotifications()

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isEmpty()
        assertThat(response.errorCode).isNull()
      }

    @Test
    fun `알림이 아예 없는 경우 빈 리스트 반환 - checkNotifications`() {
      // given - 알림 없음

      // when
      val response = checkNotifications()

      // then
      assertThat(response.statusCode).isEqualTo(200)
      assertThat(response.data).isEmpty()
      assertThat(response.errorCode).isNull()
    }

    @Test
    fun `삭제된 알림은 처리되지 않는지 확인 - checkNotifications`() =
      runTest {
        // given - 정상 미확인 알림 1개, 삭제된 미확인 알림 1개
        val normalNotification =
          createTestNotification(
            testUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Normal Notification",
            "Normal description",
            checked = false,
          )
        val deletedNotification =
          createTestNotification(
            testUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Deleted Notification",
            "Deleted description",
            checked = false,
            deletedAt = Instant.now(),
          )

        // when
        val response = checkNotifications()

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).hasSize(1)
        assertThat(response.data!!.first().id).isEqualTo(normalNotification.id)
        assertThat(response.errorCode).isNull()

        // 삭제된 알림의 상태는 변경되지 않았는지 확인
        val unchangedDeleted = notificationRepository.findById(deletedNotification.id!!)
        assertThat(unchangedDeleted!!.checked).isFalse()
        assertThat(unchangedDeleted.deletedAt).isNotNull()
      }

    @Test
    fun `다른 사용자의 알림은 처리되지 않는지 확인 - checkNotifications`() =
      runTest {
        // given - testUser 미확인 알림 1개, otherUser 미확인 알림 1개
        val myNotification =
          createTestNotification(
            testUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "My Notification",
            "My description",
            checked = false,
          )
        val otherUserNotification =
          createTestNotification(
            otherUser,
            NotificationType.ASSISTANT_FEEDBACK,
            "Other User's Notification",
            "Other user's description",
            checked = false,
          )

        // when - testUser로 요청
        val response = checkNotifications()

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).hasSize(1)
        assertThat(response.data!!.first().id).isEqualTo(myNotification.id)
        assertThat(response.data!!.first().receiverId).isEqualTo(testUser.id)
        assertThat(response.errorCode).isNull()

        // 다른 사용자의 알림 상태는 변경되지 않았는지 확인
        val unchangedOtherNotification = notificationRepository.findById(otherUserNotification.id!!)
        assertThat(unchangedOtherNotification!!.checked).isFalse()
        assertThat(unchangedOtherNotification.receiverId).isEqualTo(otherUser.id)
      }

    @Test
    fun `인증되지 않은 사용자 요청 시 401 에러 - checkNotifications`() =
      runTest {
        // given - 테스트 알림 생성
        createTestNotification(testUser, checked = false)

        // when
        val response = checkNotificationsExpectingError(null)

        // then
        verifyErrorResponse(response, 401, "AUTH:UNAUTHORIZED")
      }

    @Test
    fun `Bearer Token으로 모든 알림 확인 테스트`() =
      runTest {
        // given - 미확인 알림 2개 생성
        createTestNotification(
          testUser,
          NotificationType.ASSISTANT_FEEDBACK,
          "Bearer Token Test 1",
          "Test 1 with bearer token",
          checked = false,
        )
        createTestNotification(
          testUser,
          NotificationType.ASSISTANT_FEEDBACK,
          "Bearer Token Test 2",
          "Test 2 with bearer token",
          checked = false,
        )
        val validAccessToken = tokenService.createAccessToken(testUser)

        // when
        val response =
          webTestClient
            .patch()
            .uri("/api/v1/notifications/check")
            .accept(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $validAccessToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<List<Notification>>>()
            .returnResult()
            .responseBody!!

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).hasSize(2)
        assertThat(response.data!!.all { it.checked }).isTrue()
        assertThat(response.errorCode).isNull()
      }

    @Test
    fun `존재하지 않는 사용자로 요청 시 빈 결과 반환 - checkNotifications`() =
      runTest {
        // given - 존재하지 않는 사용자
        val nonExistentUser =
          User(
            id = UUID.randomUUID(),
            provider = OAuth2Provider.GOOGLE,
            providerId = "non-existent-user",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
          )
        val nonExistentAuth =
          UsernamePasswordAuthenticationToken(
            nonExistentUser,
            null,
            listOf(SimpleGrantedAuthority(Role.USER.name)),
          )

        // testUser의 미확인 알림 생성 (다른 사용자이므로 처리되지 않아야 함)
        createTestNotification(testUser, checked = false)

        // when
        val response = checkNotifications(nonExistentAuth)

        // then - 존재하지 않는 사용자의 경우 빈 결과 반환
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isEmpty()
        assertThat(response.errorCode).isNull()
      }

    private fun checkNotifications(
      auth: UsernamePasswordAuthenticationToken = authentication,
    ): ServiceResponse<List<Notification>> =
      webTestClient
        .mutateWith(mockAuthentication(auth))
        .patch()
        .uri("/api/v1/notifications/check")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ServiceResponse<List<Notification>>>()
        .returnResult()
        .responseBody!!

    private fun checkNotificationsExpectingError(auth: UsernamePasswordAuthenticationToken?): ServiceResponse<Unit> {
      val testClient =
        if (auth != null) {
          webTestClient.mutateWith(mockAuthentication(auth))
        } else {
          webTestClient
        }

      return testClient
        .patch()
        .uri("/api/v1/notifications/check")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody<ServiceResponse<Unit>>()
        .returnResult()
        .responseBody!!
    }

    private fun verifyNotificationConsistency(
      actual: Notification,
      expected: NotificationEntity,
    ) {
      assertThat(actual.id).isEqualTo(expected.id)
      assertThat(actual.type).isEqualTo(expected.type)
      assertThat(actual.title).isEqualTo(expected.title)
      assertThat(actual.description).isEqualTo(expected.description)
      assertThat(actual.metadata).isEqualTo(expected.metadata)
      assertThat(actual.receiverId).isEqualTo(expected.receiverId)
      assertThat(actual.checked).isEqualTo(expected.checked)
      assertThat(actual.deletedAt).isEqualTo(expected.deletedAt)
    }
  }
