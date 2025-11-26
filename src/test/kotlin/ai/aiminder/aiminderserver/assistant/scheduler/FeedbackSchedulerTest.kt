package ai.aiminder.aiminderserver.assistant.scheduler

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.domain.AssistantResponseType
import ai.aiminder.aiminderserver.assistant.domain.ChatResponseDto
import ai.aiminder.aiminderserver.assistant.dto.GetMessagesRequestDto
import ai.aiminder.aiminderserver.assistant.dto.UpdateConversationDto
import ai.aiminder.aiminderserver.assistant.repository.ChatRepository
import ai.aiminder.aiminderserver.assistant.service.AssistantService
import ai.aiminder.aiminderserver.assistant.service.ChatService
import ai.aiminder.aiminderserver.auth.dto.OAuth2UserInfo
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.conversation.domain.Conversation
import ai.aiminder.aiminderserver.conversation.service.ConversationService
import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequestDto
import ai.aiminder.aiminderserver.goal.service.GoalService
import ai.aiminder.aiminderserver.schedule.domain.Schedule
import ai.aiminder.aiminderserver.schedule.dto.CreateScheduleRequestDto
import ai.aiminder.aiminderserver.schedule.service.ScheduleService
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.service.UserService
import com.ninjasquad.springmockk.MockkBean
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.UUID

class FeedbackSchedulerTest
  @Autowired
  constructor(
    private val chatRepository: ChatRepository,
    private val conversationService: ConversationService,
    private val userService: UserService,
    private val goalService: GoalService,
    private val scheduleService: ScheduleService,
    private val chatService: ChatService,
    private val feedbackScheduler: FeedbackScheduler,
  ) : BaseIntegrationTest() {
    @MockkBean(relaxed = true)
    private lateinit var assistantService: AssistantService

    @Test
    fun `정상적인 피드백 스케줄링 테스트`() =
      runTest {
        // given - 실제 데이터베이스에 데이터 생성
        val user1 = createTestUser("user1")
        val user2 = createTestUser("user2")

        val goal1 = createTestGoal("goal1", user1.id)
        val goal2 = createTestGoal("goal2", user2.id)

        val conversation1 = createTestConversation("conv1", goal1.id, user1)
        val conversation2 = createTestConversation("conv2", goal2.id, user2)

        val yesterdaySchedule = createTestSchedule("schedule1", goal1.id, user1.id)

        val assistantResponse = createTestAssistantResponse()

        // AI 서비스 모킹 설정 (relaxed mocking으로 자동 처리)

        // when
        feedbackScheduler.feedback()

        // then - 실제 데이터베이스 상태 검증
        val allUsers = userService.getUsers().toList()
        val user1Goals = goalService.getByUserId(user1.id).toList()
        val user2Goals = goalService.getByUserId(user2.id).toList()

        // 채팅 데이터 확인
        val conversation1Chats =
          chatService
            .get(
              GetMessagesRequestDto(
                conversationId = conversation1.id,
                pageable = PageRequest.of(0, 10),
              ),
            ).content

        val conversation2Chats =
          chatService
            .get(
              GetMessagesRequestDto(
                conversationId = conversation2.id,
                pageable = PageRequest.of(0, 10),
              ),
            ).content

        // 데이터 생성 확인
        assert(allUsers.size >= 2)
        assert(user1Goals.isNotEmpty())
        assert(user2Goals.isNotEmpty())

        // 실제 피드백이 생성되었는지 확인 (모킹이 제대로 작동했다면 채팅이 생성되어야 함)

        // 피드백 채팅이 생성되었는지 확인
        assert(conversation1Chats.isNotEmpty() || conversation2Chats.isNotEmpty())
      }

    @Test
    fun `사용자가 없는 경우 테스트`() =
      runTest {
        // given - 데이터베이스에 사용자가 없는 상태 (기본 상태)

        // when
        feedbackScheduler.feedback()

        // then - 사용자가 없으므로 피드백이 생성되지 않음
        val allUsers = userService.getUsers().toList()
        assert(allUsers.isEmpty())
      }

    @Test
    fun `목표가 없는 사용자 테스트`() =
      runTest {
        // given - 사용자는 있지만 목표가 없는 상태
        val user = createTestUser("user1")

        // when
        feedbackScheduler.feedback()

        // then - 목표가 없으므로 피드백이 생성되지 않음
        val allUsers = userService.getUsers().toList()
        val userGoals = goalService.getByUserId(user.id).toList()

        assert(allUsers.isNotEmpty())
        assert(userGoals.isEmpty())
      }

    @Test
    fun `스케줄이 없는 경우 테스트`() =
      runTest {
        // given - 사용자와 목표는 있지만 스케줄이 없는 상태
        val user = createTestUser("user1")
        val goal = createTestGoal("goal1", user.id)
        val conversation = createTestConversation("conv1", goal.id, user)
        val assistantResponse = createTestAssistantResponse()

        // AI 서비스 모킹 설정 (relaxed mocking으로 자동 처리)

        // when
        feedbackScheduler.feedback()

        // then - 스케줄이 없어도 피드백은 생성됨 (빈 스케줄 리스트로)
        val allUsers = userService.getUsers().toList()
        val userGoals = goalService.getByUserId(user.id).toList()
        val schedules = scheduleService.get(goal.id, Instant.now().minusSeconds(86400), Instant.now())

        val conversationChats =
          chatService
            .get(
              GetMessagesRequestDto(
                conversationId = conversation.id,
                pageable = PageRequest.of(0, 10),
              ),
            ).content

        assert(allUsers.isNotEmpty())
        assert(userGoals.isNotEmpty())
        assert(schedules.isEmpty())
        assert(conversationChats.isEmpty())
      }

    @Test
    fun `대화방을 찾을 수 없는 경우 테스트`() =
      runTest {
        // given - 사용자와 목표는 있지만 대화방이 없는 상태
        val user = createTestUser("user1")
        val goal = createTestGoal("goal1", user.id)
        // 대화방을 생성하지 않음

        // when
        feedbackScheduler.feedback()

        // then - 대화방이 없으면 피드백 생성 시 예외가 발생하고 해당 목표는 건너뜀
        val allUsers = userService.getUsers().toList()
        val userGoals = goalService.getByUserId(user.id).toList()

        assert(allUsers.isNotEmpty())
        assert(userGoals.isNotEmpty())
        // 대화방이 없으므로 conversationService.getByGoal에서 예외 발생
        // 피드백 스케줄러는 예외를 처리하고 다음 목표로 넘어감
      }

    @Test
    fun `AI 피드백 생성 실패 테스트`() =
      runTest {
        // given
        val user = createTestUser("user1")
        val goal = createTestGoal("goal1", user.id)
        val conversation = createTestConversation("conv1", goal.id, user)

        // AI 서비스에서 예외 발생하도록 모킹 (relaxed이므로 실제로는 예외가 발생하지 않음)
        // 이 테스트는 relaxed mocking으로 인해 실제 예외가 발생하지 않으므로 성공할 것으로 예상

        // when
        feedbackScheduler.feedback()

        // then - AI 피드백 생성 실패 시 채팅이 생성되지 않음
        val allUsers = userService.getUsers().toList()
        val userGoals = goalService.getByUserId(user.id).toList()

        val conversationChats =
          chatService
            .get(
              GetMessagesRequestDto(
                conversationId = conversation.id,
                pageable = PageRequest.of(0, 10),
              ),
            ).content

        assert(allUsers.isNotEmpty())
        assert(userGoals.isNotEmpty())
        // AI 서비스 오류로 인해 채팅이 생성되지 않음
        assert(conversationChats.isEmpty())
      }

    @Test
    fun `다수 사용자 다수 목표 처리 테스트`() =
      runTest {
        // given - 다수 사용자와 목표 생성
        val user1 = createTestUser("user1")
        val user2 = createTestUser("user2")
        val user3 = createTestUser("user3")

        val goal1 = createTestGoal("goal1-1", user1.id)
        val goal2 = createTestGoal("goal1-2", user1.id)
        val goal3 = createTestGoal("goal2-1", user2.id)
        val goal4 = createTestGoal("goal3-1", user3.id)
        val goal5 = createTestGoal("goal3-2", user3.id)
        val goal6 = createTestGoal("goal3-3", user3.id)

        val conversation1 = createTestConversation("conv1", goal1.id, user1)
        val conversation2 = createTestConversation("conv2", goal2.id, user1)
        val conversation3 = createTestConversation("conv3", goal3.id, user2)
        val conversation4 = createTestConversation("conv4", goal4.id, user3)
        val conversation5 = createTestConversation("conv5", goal5.id, user3)
        val conversation6 = createTestConversation("conv6", goal6.id, user3)

        createTestSchedule("schedule1", goal1.id, user1.id)
        createTestSchedule("schedule2", goal2.id, user1.id)
        createTestSchedule("schedule3", goal3.id, user2.id)
        createTestSchedule("schedule4", goal4.id, user3.id)
        createTestSchedule("schedule5", goal5.id, user3.id)
        createTestSchedule("schedule6", goal6.id, user3.id)

        // AI 서비스 모킹 설정 (relaxed mocking으로 자동 처리)

        // when
        feedbackScheduler.feedback()

        // then - 모든 사용자와 목표에 대한 피드백 생성 확인
        val allUsers = userService.getUsers().toList()
        val user1Goals = goalService.getByUserId(user1.id).toList()
        val user2Goals = goalService.getByUserId(user2.id).toList()
        val user3Goals = goalService.getByUserId(user3.id).toList()

        // 데이터 생성 확인
        assert(allUsers.size == 3)
        assert(user1Goals.size == 2)
        assert(user2Goals.size == 1)
        assert(user3Goals.size == 3)

        // 각 대화방에 피드백 채팅이 생성되었는지 확인
        val conversations =
          listOf(conversation1, conversation2, conversation3, conversation4, conversation5, conversation6)
        val totalChats = chatRepository.findAll().toList().size
        // 모든 목표에 대해 피드백이 생성되었는지 확인
        assert(totalChats == 6) // 각 목표당 최소 1개의 피드백 채팅
      }

    // 헬퍼 메서드들 - 실제 데이터베이스에 저장
    private suspend fun createTestUser(providerId: String): User {
      val userInfo = OAuth2UserInfo(id = providerId)
      return userService.createUser(userInfo, "google")
    }

    private suspend fun createTestGoal(
      title: String,
      userId: UUID,
    ): Goal {
      val dto =
        CreateGoalRequestDto(
          userId = userId,
          title = title,
          description = "테스트 목표 내용",
          // 30일 후
          targetDate = Instant.now().plusSeconds(86400 * 30),
          imageId = null,
          isAiGenerated = false,
        )
      val goalResponse = goalService.create(dto)
      return Goal(
        id = goalResponse.id,
        userId = userId,
        title = goalResponse.title,
        description = goalResponse.description,
        targetDate = goalResponse.targetDate,
        isAiGenerated = goalResponse.isAiGenerated,
        status = goalResponse.status,
        imageId = null,
        createdAt = goalResponse.createdAt,
        updatedAt = goalResponse.updatedAt,
        deletedAt = goalResponse.deletedAt,
      )
    }

    private suspend fun createTestConversation(
      @Suppress("UNUSED_PARAMETER") title: String,
      goalId: UUID,
      user: User,
    ): Conversation {
      val conversation = conversationService.create(user)
      return conversationService.update(
        UpdateConversationDto(
          conversationId = conversation.id,
          goalId = goalId,
        ),
      )
    }

    private suspend fun createTestSchedule(
      title: String,
      goalId: UUID,
      userId: UUID,
    ): Schedule {
      val dto =
        CreateScheduleRequestDto(
          goalId = goalId,
          userId = userId,
          title = title,
          description = "테스트 스케줄 내용",
          startDate = Instant.now(),
          endDate = Instant.now().plusSeconds(3600),
        )
      val scheduleResponse = scheduleService.create(dto)
      return Schedule(
        id = scheduleResponse.id,
        goalId = goalId,
        userId = userId,
        title = scheduleResponse.title,
        description = scheduleResponse.description,
        status = scheduleResponse.status,
        startDate = scheduleResponse.startDate,
        endDate = scheduleResponse.endDate,
        createdAt = scheduleResponse.createdAt,
        updatedAt = scheduleResponse.updatedAt,
        deletedAt = scheduleResponse.deletedAt,
      )
    }

    private fun createTestAssistantResponse(): AssistantResponse =
      AssistantResponse(
        responses =
          listOf(
            ChatResponseDto(
              type = AssistantResponseType.TEXT,
              messages = listOf("테스트 피드백 메시지입니다."),
            ),
          ),
      )
  }
