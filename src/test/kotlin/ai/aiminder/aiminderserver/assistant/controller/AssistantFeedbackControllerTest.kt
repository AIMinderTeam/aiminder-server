package ai.aiminder.aiminderserver.assistant.controller

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.domain.AssistantResponseType
import ai.aiminder.aiminderserver.assistant.domain.ChatResponseDto
import ai.aiminder.aiminderserver.assistant.domain.ChatType
import ai.aiminder.aiminderserver.assistant.dto.ChatResponse
import ai.aiminder.aiminderserver.assistant.repository.ChatRepository
import ai.aiminder.aiminderserver.assistant.service.AssistantService
import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.common.util.toUtcInstant
import ai.aiminder.aiminderserver.conversation.domain.Conversation
import ai.aiminder.aiminderserver.conversation.entity.ConversationEntity
import ai.aiminder.aiminderserver.conversation.repository.ConversationRepository
import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.domain.GoalStatus
import ai.aiminder.aiminderserver.goal.entity.GoalEntity
import ai.aiminder.aiminderserver.goal.repository.GoalRepository
import ai.aiminder.aiminderserver.schedule.domain.Schedule
import ai.aiminder.aiminderserver.schedule.domain.ScheduleStatus
import ai.aiminder.aiminderserver.schedule.entity.ScheduleEntity
import ai.aiminder.aiminderserver.schedule.repository.ScheduleRepository
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.entity.UserEntity
import ai.aiminder.aiminderserver.user.repository.UserRepository
import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication
import org.springframework.test.web.reactive.server.expectBody
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class AssistantFeedbackControllerTest
  @Autowired
  constructor(
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository,
    private val chatRepository: ChatRepository,
    private val goalRepository: GoalRepository,
    private val scheduleRepository: ScheduleRepository,
  ) : BaseIntegrationTest() {
    @MockkBean
    private lateinit var assistantService: AssistantService

    private lateinit var testUser: User
    private lateinit var authentication: UsernamePasswordAuthenticationToken

    @BeforeEach
    fun setUp() =
      runTest {
        // Clear all mocks before each test
        clearMocks(assistantService)

        val savedUser =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.GOOGLE,
              providerId = "test-feedback-user-123",
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

    // Feedback API 테스트들
    @Test
    fun `정상적인 피드백 요청 테스트`() =
      runTest {
        // given - 테스트 데이터 준비
        val (goal, yesterdaySchedules, todaySchedules) = createTestGoalWithSchedules()
        val conversation = createTestConversationWithGoal(goal)

        // AssistantService.feedback 모킹
        val expectedAssistantResponse =
          AssistantResponse(
            responses =
              listOf(
                ChatResponseDto(
                  type = AssistantResponseType.TEXT,
                  messages =
                    listOf(
                      "어제 완료하신 '온라인 강의 수강'과 '투자서적 읽기' 정말 수고하셨습니다! " +
                        "오늘 예정된 '부동산 매물 조사'와 '투자 계획 수립'도 차근차근 진행해보세요. " +
                        "목표 달성을 위한 꾸준한 노력이 보기 좋습니다.",
                    ),
                ),
                ChatResponseDto(
                  type = AssistantResponseType.QUICK_REPLIES,
                  messages = listOf("오늘 일정 시작하기", "목표 수정하기", "새 일정 추가하기"),
                ),
              ),
          )

        coEvery {
          assistantService.feedback(
            user = testUser,
            conservation = Conversation.from(conversation),
            goal = Goal.from(goal),
            yesterdaySchedules = yesterdaySchedules.map { Schedule.fromEntity(it) },
            todaySchedules = todaySchedules.map { Schedule.fromEntity(it) },
          )
        } returns expectedAssistantResponse

        // when - 피드백 요청
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/${conversation.id}/feedback")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<ChatResponse>>()
            .returnResult()
            .responseBody!!

        // then - 응답 검증
        response.also {
          assertThat(it.statusCode).isEqualTo(200)
          assertThat(it.data?.conversationId).isEqualTo(conversation.id)
          assertThat(it.data?.chatType).isEqualTo(ChatType.ASSISTANT)
          assertThat(it.data?.chat).hasSize(2) // TEXT + QUICK_REPLIES
          assertThat(
            it.data
              ?.chat
              ?.get(0)
              ?.messages
              ?.get(0),
          ).contains("온라인 강의 수강", "투자서적 읽기", "부동산 매물 조사")
        }

        // FeedbackService의 실제 로직 동작 검증
        coVerify {
          assistantService.feedback(
            user = testUser,
            conservation = Conversation.from(conversation),
            goal = Goal.from(goal),
            yesterdaySchedules = yesterdaySchedules.map { Schedule.fromEntity(it) },
            todaySchedules = todaySchedules.map { Schedule.fromEntity(it) },
          )
        }

        // 채팅 저장 검증
        val savedChats = chatRepository.findAll().toList()
        assertThat(savedChats).hasSize(1)
        assertThat(savedChats.first().type).isEqualTo(ChatType.ASSISTANT)
        assertThat(savedChats.first().conversationId).isEqualTo(conversation.id)
      }

    @Test
    fun `스케줄이 없는 경우 피드백 테스트`() =
      runTest {
        // given - 스케줄이 없는 Goal만 생성
        val goal = createTestGoalWithoutSchedules()
        val conversation = createTestConversationWithGoal(goal)

        // AssistantService.feedback 모킹 - 스케줄이 없는 경우
        val expectedAssistantResponse =
          AssistantResponse(
            responses =
              listOf(
                ChatResponseDto(
                  type = AssistantResponseType.TEXT,
                  messages = listOf("피드백 할 일정이 존재하지 않습니다."),
                ),
              ),
          )

        coEvery {
          assistantService.feedback(
            user = testUser,
            conservation = Conversation.from(conversation),
            goal = Goal.from(goal),
            yesterdaySchedules = emptyList(),
            todaySchedules = emptyList(),
          )
        } returns expectedAssistantResponse

        // when & then
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/${conversation.id}/feedback")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<ChatResponse>>()
            .returnResult()
            .responseBody!!

        assertThat(
          response.data
            ?.chat
            ?.get(0)
            ?.messages
            ?.get(0),
        ).isEqualTo("피드백 할 일정이 존재하지 않습니다.")

        // 파라미터 검증
        coVerify {
          assistantService.feedback(
            user = testUser,
            conservation = Conversation.from(conversation),
            goal = Goal.from(goal),
            yesterdaySchedules = emptyList(),
            todaySchedules = emptyList(),
          )
        }
      }

    @Test
    fun `인증되지 않은 사용자 피드백 요청 시 401 반환`() =
      runTest {
        // given
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )

        // when
        val response =
          webTestClient
            .post()
            .uri("/api/v1/conversations/${conversation.id}/feedback")
            .exchange()
            .expectStatus()
            .isUnauthorized
            .expectBody<ServiceResponse<Unit>>()
            .returnResult()
            .responseBody!!

        // then
        assertThat(response.statusCode).isEqualTo(401)
        assertThat(response.message).isEqualTo("인증이 필요합니다. 로그인을 진행해주세요.")
        assertThat(response.errorCode).isEqualTo("AUTH:UNAUTHORIZED")
        assertThat(response.data).isNull()
      }

    @Test
    fun `다른 사용자의 대화방에 피드백 요청 시 401 반환`() =
      runTest {
        // given - 다른 사용자 생성
        val anotherUser =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.KAKAO,
              providerId = "another-user-feedback-789",
            ),
          )

        val anotherUserDomain = User.from(anotherUser)
        val anotherUserConversation =
          conversationRepository.save(
            ConversationEntity.from(anotherUserDomain),
          )

        // when - 다른 사용자의 대화방에 피드백 요청 시도
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/${anotherUserConversation.id}/feedback")
            .exchange()
            .expectStatus()
            .isUnauthorized
            .expectBody<ServiceResponse<Unit>>()
            .returnResult()
            .responseBody!!

        // then
        assertThat(response.statusCode).isEqualTo(401)
        assertThat(response.errorCode).isEqualTo("AUTH:UNAUTHORIZED")
        assertThat(response.message).isEqualTo("인증이 필요합니다. 로그인을 진행해주세요.")
        assertThat(response.data).isNull()
      }

    @Test
    fun `존재하지 않는 대화방에 피드백 요청 시 404 반환`() =
      runTest {
        // given
        val nonExistentConversationId = UUID.randomUUID()

        // when
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/$nonExistentConversationId/feedback")
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody<ServiceResponse<Unit>>()
            .returnResult()
            .responseBody!!

        // then
        assertThat(response.statusCode).isEqualTo(404)
        assertThat(response.errorCode).isEqualTo("ASSISTANT:CONVERSATIONNOTFOUND")
        assertThat(response.message).contains("대화방을 찾을 수 없습니다")
      }

    @Test
    fun `잘못된 UUID 형식으로 피드백 요청 시 400 반환`() {
      // given
      val invalidUuid = "invalid-uuid-format"

      // when
      val response =
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/conversations/$invalidUuid/feedback")
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody<ServiceResponse<Unit>>()
          .returnResult()
          .responseBody!!

      // then
      assertThat(response.statusCode).isEqualTo(400)
      assertThat(response.errorCode).isEqualTo("COMMON:INVALIDREQUEST")
    }

    // Test data creation helper methods
    private suspend fun createTestGoalWithSchedules(): Triple<GoalEntity, List<ScheduleEntity>, List<ScheduleEntity>> {
      val goal = createTestGoal()
      val yesterdaySchedules = createYesterdaySchedules(goal.id!!)
      val todaySchedules = createTodaySchedules(goal.id!!)
      return Triple(goal, yesterdaySchedules, todaySchedules)
    }

    private suspend fun createTestGoal(): GoalEntity {
      val goalEntity =
        GoalEntity(
          userId = testUser.id,
          title = "피드백 테스트용 목표",
          description = "월 300만원 수입 달성하기",
          targetDate = Instant.now().plus(Duration.ofDays(90)),
          isAiGenerated = false,
          status = GoalStatus.INPROGRESS,
        )
      return goalRepository.save(goalEntity)
    }

    private suspend fun createTestConversationWithGoal(goal: GoalEntity): ConversationEntity {
      val conversationEntity =
        ConversationEntity(
          userId = testUser.id,
          goalId = goal.id,
        )
      return conversationRepository.save(conversationEntity)
    }

    private suspend fun createYesterdaySchedules(goalId: UUID): List<ScheduleEntity> {
      val yesterday = LocalDate.now().minusDays(1)

      val schedule1 =
        ScheduleEntity(
          goalId = goalId,
          userId = testUser.id,
          title = "온라인 강의 수강",
          description = "부동산 투자 기초 강의 3시간",
          status = ScheduleStatus.COMPLETED,
          startDate = yesterday.atTime(9, 0).toUtcInstant(),
          endDate = yesterday.atTime(9, 0).toUtcInstant(),
        )

      val schedule2 =
        ScheduleEntity(
          goalId = goalId,
          userId = testUser.id,
          title = "투자서적 읽기",
          description = "부의 추월차선 1-3장",
          status = ScheduleStatus.COMPLETED,
          startDate = yesterday.atTime(9, 0).toUtcInstant(),
          endDate = yesterday.atTime(9, 0).toUtcInstant(),
        )

      return listOf(
        scheduleRepository.save(schedule1),
        scheduleRepository.save(schedule2),
      )
    }

    private suspend fun createTodaySchedules(goalId: UUID): List<ScheduleEntity> {
      val today = LocalDate.now()

      val schedule1 =
        ScheduleEntity(
          goalId = goalId,
          userId = testUser.id,
          title = "부동산 매물 조사",
          description = "강남구 오피스텔 시세 조사",
          status = ScheduleStatus.READY,
          startDate = today.atTime(9, 0).toUtcInstant(),
          endDate = today.atTime(9, 0).toUtcInstant(),
        )

      val schedule2 =
        ScheduleEntity(
          goalId = goalId,
          userId = testUser.id,
          title = "투자 계획 수립",
          description = "1000만원 투자 포트폴리오 계획",
          status = ScheduleStatus.READY,
          startDate = today.atTime(9, 0).toUtcInstant(),
          endDate = today.atTime(9, 0).toUtcInstant(),
        )

      return listOf(
        scheduleRepository.save(schedule1),
        scheduleRepository.save(schedule2),
      )
    }

    private suspend fun createTestGoalWithoutSchedules(): GoalEntity {
      val goalEntity =
        GoalEntity(
          userId = testUser.id,
          title = "스케줄이 없는 테스트 목표",
          description = "피드백 테스트를 위한 목표",
          targetDate = Instant.now().plus(Duration.ofDays(30)),
          isAiGenerated = false,
          status = GoalStatus.INPROGRESS,
        )
      return goalRepository.save(goalEntity)
    }
  }
