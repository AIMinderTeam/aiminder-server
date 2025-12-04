package ai.aiminder.aiminderserver.notification.event

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.notification.domain.NotificationType
import ai.aiminder.aiminderserver.notification.dto.CreateNotificationRequest
import ai.aiminder.aiminderserver.notification.repository.NotificationRepository
import ai.aiminder.aiminderserver.notification.service.NotificationService
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.entity.UserEntity
import ai.aiminder.aiminderserver.user.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import java.util.UUID

class NotificationEventListenerTest
  @Autowired
  constructor(
    private val notificationEventListener: NotificationEventListener,
    private val notificationService: NotificationService,
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
  ) : BaseIntegrationTest() {
    private lateinit var testUser: User

    @BeforeEach
    fun setUp() =
      runTest {
        val savedUser =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.GOOGLE,
              providerId = "test-event-listener-123",
            ),
          )
        testUser = User.from(savedUser)
      }

    private suspend fun waitForNotificationCount(
      userId: UUID,
      expectedCount: Long,
    ) {
      var attempts = 0
      val maxAttempts = 50
      var currentCount = 0L

      while (attempts < maxAttempts && currentCount < expectedCount) {
        delay(100)
        currentCount = notificationRepository.countByReceiverIdAndDeletedAtIsNull(userId)
        attempts++
      }

      assertThat(currentCount).isEqualTo(expectedCount)
    }

    @Test
    fun `NotificationService 직접 호출 테스트`() =
      runTest {
        // given
        val request =
          CreateNotificationRequest(
            title = "Direct Test",
            content = "Direct service call test",
            note = mapOf("key" to "value"),
            receiverId = testUser.id,
            type = NotificationType.ASSISTANT_FEEDBACK,
          )
        val initialCount = notificationRepository.countByReceiverIdAndDeletedAtIsNull(testUser.id)

        // when
        notificationService.create(request)

        // then
        val finalCount = notificationRepository.countByReceiverIdAndDeletedAtIsNull(testUser.id)
        assertThat(finalCount).isEqualTo(initialCount + 1L)
      }

    @Test
    fun `CreateNotificationRequest from 변환 테스트`() =
      runTest {
        // given
        val testEvent = createTestEvent(receiverId = testUser.id, goalTitle = "Test Goal")

        // when & then - 예외가 발생하지 않는지 확인
        val request = CreateNotificationRequest.from(testEvent)

        assertThat(request.receiverId).isEqualTo(testUser.id)
        assertThat(request.title).isEqualTo("AI 비서 알림")
        assertThat(request.content).isEqualTo("\"Test Goal\" 목표에 대한 피드백을 확인하세요.")
        assertThat(request.type).isEqualTo(NotificationType.ASSISTANT_FEEDBACK)
        assertThat(request.note).isNotNull()
        assertThat(request.note).containsKey("receiverId")
        assertThat(request.note).containsKey("goalTitle")
        assertThat(request.note).containsKey("conversationId")
        assertThat(request.note).containsKey("type")
      }

    @Test
    fun `정상적인 알림 생성 이벤트 처리 테스트`() =
      runTest {
        // given
        val testEvent = createTestEvent(receiverId = testUser.id, goalTitle = "운동 목표")
        val initialCount = notificationRepository.countByReceiverIdAndDeletedAtIsNull(testUser.id)

        // when
        notificationEventListener.consumeCreateDomainEvent(testEvent)

        // 비동기 처리 완료를 위해 반복 확인
        var attempts = 0
        val maxAttempts = 50
        var finalCount = initialCount

        while (attempts < maxAttempts && finalCount == initialCount) {
          delay(100)
          finalCount = notificationRepository.countByReceiverIdAndDeletedAtIsNull(testUser.id)
          attempts++
        }

        // then
        assertThat(finalCount).isEqualTo(initialCount + 1L)

        val notifications =
          notificationRepository.findAllByReceiverIdAndDeletedAtIsNull(
            testUser.id,
            PageRequest.of(0, 100),
          ).toList()
        assertThat(notifications).isNotEmpty()

        val createdNotification = notifications.first()
        assertThat(createdNotification.title).isEqualTo("AI 비서 알림")
        assertThat(createdNotification.description).isEqualTo("\"운동 목표\" 목표에 대한 피드백을 확인하세요.")
        assertThat(createdNotification.receiverId).isEqualTo(testUser.id)
        assertThat(createdNotification.type).isEqualTo(NotificationType.ASSISTANT_FEEDBACK)
        assertThat(createdNotification.checked).isFalse()
        assertThat(createdNotification.deletedAt).isNull()
      }

    @Test
    fun `여러 이벤트 동시 처리 테스트`() =
      runTest {
        // given
        val event1 = createTestEvent(receiverId = testUser.id, goalTitle = "운동 목표")
        val event2 = createTestEvent(receiverId = testUser.id, goalTitle = "독서 목표")
        val event3 = createTestEvent(receiverId = testUser.id, goalTitle = "학습 목표")
        val initialCount = notificationRepository.countByReceiverIdAndDeletedAtIsNull(testUser.id)

        // when
        notificationEventListener.consumeCreateDomainEvent(event1)
        notificationEventListener.consumeCreateDomainEvent(event2)
        notificationEventListener.consumeCreateDomainEvent(event3)

        // then
        waitForNotificationCount(testUser.id, initialCount + 3L)

        val notifications =
          notificationRepository.findAllByReceiverIdAndDeletedAtIsNull(
            testUser.id,
            PageRequest.of(0, 100),
          ).toList()
        val descriptions = notifications.map { it.description }
        assertThat(descriptions).contains(
          "\"운동 목표\" 목표에 대한 피드백을 확인하세요.",
          "\"독서 목표\" 목표에 대한 피드백을 확인하세요.",
          "\"학습 목표\" 목표에 대한 피드백을 확인하세요.",
        )
        // 모든 알림의 title은 동일해야 함
        notifications.forEach { notification ->
          assertThat(notification.title).isEqualTo("AI 비서 알림")
          assertThat(notification.type).isEqualTo(NotificationType.ASSISTANT_FEEDBACK)
        }
      }

    @Test
    fun `빈 제목과 내용이 포함된 이벤트 처리 테스트`() =
      runTest {
        // given
        val testEvent = createTestEvent(receiverId = testUser.id, goalTitle = "")
        val initialCount = notificationRepository.countByReceiverIdAndDeletedAtIsNull(testUser.id)

        // when
        notificationEventListener.consumeCreateDomainEvent(testEvent)

        // then
        waitForNotificationCount(testUser.id, initialCount + 1L)

        val notifications =
          notificationRepository.findAllByReceiverIdAndDeletedAtIsNull(
            testUser.id,
            PageRequest.of(0, 100),
          ).toList()
        val createdNotification = notifications.first()

        assertThat(createdNotification.title).isEqualTo("AI 비서 알림")
        assertThat(createdNotification.description).isEqualTo("\"\" 목표에 대한 피드백을 확인하세요.")
        assertThat(createdNotification.receiverId).isEqualTo(testUser.id)
        assertThat(createdNotification.type).isEqualTo(NotificationType.ASSISTANT_FEEDBACK)
      }

    @Test
    fun `ASSISTANT_FEEDBACK 타입 이벤트 처리 테스트`() =
      runTest {
        // given
        val testEvent = createTestEvent(receiverId = testUser.id, goalTitle = "AI 어시스턴트 테스트")
        val initialCount = notificationRepository.countByReceiverIdAndDeletedAtIsNull(testUser.id)

        // when
        notificationEventListener.consumeCreateDomainEvent(testEvent)

        // then
        waitForNotificationCount(testUser.id, initialCount + 1L)

        val notifications =
          notificationRepository.findAllByReceiverIdAndDeletedAtIsNull(
            testUser.id,
            PageRequest.of(0, 100),
          ).toList()
        val createdNotification = notifications.first()

        assertThat(createdNotification.type).isEqualTo(NotificationType.ASSISTANT_FEEDBACK)
        assertThat(createdNotification.title).isEqualTo("AI 비서 알림")
        assertThat(createdNotification.description).isEqualTo("\"AI 어시스턴트 테스트\" 목표에 대한 피드백을 확인하세요.")
        assertThat(createdNotification.receiverId).isEqualTo(testUser.id)
      }

    @Test
    fun `note 메타데이터 생성 확인 테스트`() =
      runTest {
        // given
        val testEvent = createTestEvent(receiverId = testUser.id, goalTitle = "메타데이터 테스트")
        val initialCount = notificationRepository.countByReceiverIdAndDeletedAtIsNull(testUser.id)

        // when
        notificationEventListener.consumeCreateDomainEvent(testEvent)

        // then
        waitForNotificationCount(testUser.id, initialCount + 1L)

        val notifications =
          notificationRepository.findAllByReceiverIdAndDeletedAtIsNull(
            testUser.id,
            PageRequest.of(0, 100),
          ).toList()
        val createdNotification = notifications.first()

        // note 메타데이터가 생성되었는지 확인
        assertThat(createdNotification.metadata).isNotEmpty()
        assertThat(createdNotification.metadata).containsKey("receiverId")
        assertThat(createdNotification.metadata).containsKey("goalTitle")
        assertThat(createdNotification.metadata).containsKey("conversationId")
        assertThat(createdNotification.metadata).containsKey("type")

        // 메타데이터 값 확인
        assertThat(createdNotification.metadata["receiverId"]).isEqualTo(testUser.id.toString())
        assertThat(createdNotification.metadata["goalTitle"]).isEqualTo("메타데이터 테스트")
        assertThat(
          createdNotification.metadata["type"],
        ).isEqualTo("ai.aiminder.aiminderserver.notification.event.CreateFeedbackEvent")
        // conversationId는 UUID이므로 null이 아닌지만 확인
        assertThat(createdNotification.metadata["conversationId"]).isNotNull()
      }

    @Test
    fun `다른 사용자 이벤트 독립적 처리 테스트`() =
      runTest {
        // given - 다른 사용자 생성
        val otherUser =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.GOOGLE,
              providerId = "test-other-user-456",
            ),
          ).let { User.from(it) }

        val event1 = createTestEvent(receiverId = testUser.id, goalTitle = "첫 번째 사용자 목표")
        val event2 = createTestEvent(receiverId = otherUser.id, goalTitle = "두 번째 사용자 목표")

        val testUserInitialCount = notificationRepository.countByReceiverIdAndDeletedAtIsNull(testUser.id)
        val otherUserInitialCount = notificationRepository.countByReceiverIdAndDeletedAtIsNull(otherUser.id)

        // when
        notificationEventListener.consumeCreateDomainEvent(event1)
        notificationEventListener.consumeCreateDomainEvent(event2)

        // then
        waitForNotificationCount(testUser.id, testUserInitialCount + 1L)
        waitForNotificationCount(otherUser.id, otherUserInitialCount + 1L)

        // 각 사용자의 알림이 올바르게 생성되었는지 확인
        val testUserNotifications =
          notificationRepository.findAllByReceiverIdAndDeletedAtIsNull(
            testUser.id,
            PageRequest.of(0, 100),
          ).toList()
        val otherUserNotifications =
          notificationRepository.findAllByReceiverIdAndDeletedAtIsNull(
            otherUser.id,
            PageRequest.of(0, 100),
          ).toList()

        assertThat(testUserNotifications.first().title).isEqualTo("AI 비서 알림")
        assertThat(testUserNotifications.first().description).isEqualTo("\"첫 번째 사용자 목표\" 목표에 대한 피드백을 확인하세요.")
        assertThat(testUserNotifications.first().receiverId).isEqualTo(testUser.id)

        assertThat(otherUserNotifications.first().title).isEqualTo("AI 비서 알림")
        assertThat(otherUserNotifications.first().description).isEqualTo("\"두 번째 사용자 목표\" 목표에 대한 피드백을 확인하세요.")
        assertThat(otherUserNotifications.first().receiverId).isEqualTo(otherUser.id)
      }

    private fun createTestEvent(
      receiverId: UUID = UUID.randomUUID(),
      goalTitle: String = "Test Goal Title",
      conversationId: UUID = UUID.randomUUID(),
    ): CreateNotificationEvent =
      CreateFeedbackEvent(
        goalTitle = goalTitle,
        conversationId = conversationId,
        receiverId = receiverId,
      )
  }
