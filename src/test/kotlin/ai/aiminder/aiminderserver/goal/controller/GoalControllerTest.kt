package ai.aiminder.aiminderserver.goal.controller

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.auth.service.TokenService
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.domain.GoalStatus
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequest
import ai.aiminder.aiminderserver.goal.dto.GoalResponse
import ai.aiminder.aiminderserver.goal.dto.UpdateGoalRequest
import ai.aiminder.aiminderserver.goal.entity.GoalEntity
import ai.aiminder.aiminderserver.goal.repository.GoalRepository
import ai.aiminder.aiminderserver.schedule.domain.ScheduleStatus
import ai.aiminder.aiminderserver.schedule.entity.ScheduleEntity
import ai.aiminder.aiminderserver.schedule.repository.ScheduleRepository
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
import java.time.temporal.ChronoUnit.MILLIS
import java.util.UUID

class GoalControllerTest
  @Autowired
  constructor(
    private val userRepository: UserRepository,
    private val goalRepository: GoalRepository,
    private val tokenService: TokenService,
    private val scheduleRepository: ScheduleRepository,
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
    fun `정상적인 Goal 생성 테스트`() {
      // given & when
      val (request, response) = postCreateGoal()

      // then
      response.data?.also {
        assertThat(it.title).isEqualTo(request.title)
        assertThat(it.description).isEqualTo(request.description)
        assertThat(it.targetDate).isEqualTo(request.targetDate)
      }
    }

    @Test
    fun `인증된 회원이 잘못된 날짜 형식으로 Goal 생성 테스트`() {
      // given
      val request =
        mapOf(
          "title" to "Test Goal",
          "description" to "Test Description",
          "targetDate" to "2025-12-31",
        )

      // when
      val response =
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/goals")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody<ServiceResponse<Unit>>()
          .returnResult()
          .responseBody!!

      // then
      assertThat(response.statusCode).isEqualTo(400)
      assertThat(response.errorCode).isEqualTo("COMMON:INVALIDREQUEST")
      assertThat(response.data).isNull()
    }

    @Test
    fun `인증되지 않은 사용자 요청 시 401 반환`() {
      // given
      val request =
        CreateGoalRequest(
          title = "Test Goal",
          description = "Test Description",
          targetDate = Instant.now().plusSeconds(86400),
        )

      // when
      val response =
        webTestClient
          .post()
          .uri("/api/v1/goals")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
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
    fun `필수 필드 누락 시 400 Bad Request 반환`() {
      // given - title 필드 누락
      val requestMissingTitle =
        mapOf(
          "description" to "Valid description",
          "targetDate" to Instant.now().plusSeconds(86400).toString(),
        )

      // when
      val response =
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/goals")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(requestMissingTitle)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody<ServiceResponse<Unit>>()
          .returnResult()
          .responseBody!!

      // then
      response.also {
        assertThat(it.statusCode).isEqualTo(400)
        assertThat(it.errorCode).isEqualTo("COMMON:INVALIDREQUEST")
      }
    }

    @Test
    fun `잘못된 날짜 형식으로 요청 시 400 Bad Request 반환`() {
      // given
      val requestWithInvalidDate =
        mapOf(
          "title" to "Valid title",
          "description" to "Valid description",
          "targetDate" to "invalid-date-format",
        )

      // when & then
      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .post()
        .uri("/api/v1/goals")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestWithInvalidDate)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `존재하지 않는 사용자로 Goal 생성 시 적절한 에러 처리`() {
      // given - 데이터베이스에 존재하지 않는 사용자
      val nonExistentUser =
        User(
          id = UUID.randomUUID(),
          provider = OAuth2Provider.GOOGLE,
          providerId = "non-existent-user",
          createdAt = Instant.now(),
          updatedAt = Instant.now(),
        )

      val request =
        CreateGoalRequest(
          title = "Test Goal",
          description = "Test Description",
          targetDate = Instant.now().plusSeconds(86400),
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
          .post()
          .uri("/api/v1/goals")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is5xxServerError
          .expectBody<ServiceResponse<Unit>>()
          .returnResult()
          .responseBody!!

      // then
      response.also {
        assertThat(it.statusCode).isEqualTo(500)
        assertThat(it.errorCode).isEqualTo("COMMON:INTERNALSERVERERROR")
      }
    }

    @Test
    fun `목표 목록을 조회할 수 있다`() =
      runTest {
        // given
        val (_, createdGoal1) = postCreateGoal()
        val (_, createdGoal2) = postCreateGoal()
        val (_, createdGoal3) = postCreateGoal()

        goalRepository.save(
          GoalEntity(
            userId = testUser.id,
            title = "title1",
            targetDate = Instant.now(),
            status = GoalStatus.INPROGRESS,
          ),
        )
        goalRepository.save(
          GoalEntity(
            userId = testUser.id,
            title = "title1",
            targetDate = Instant.now(),
            status = GoalStatus.COMPLETED,
          ),
        )

        // when
        val response = getGoals("/api/v1/goals")

        // then
        response.data!!.also {
          assertThat(it).hasSize(3)
          val createdGoalData1 = createdGoal1.data!!
          val createdGoalData2 = createdGoal2.data!!
          val createdGoalData3 = createdGoal3.data!!
          val foundGoal1 = it[0]
          val foundGoal2 = it[1]
          val foundGoal3 = it[2]
          verifyGoalConsistency(foundGoal1, createdGoalData3)
          verifyGoalConsistency(foundGoal2, createdGoalData2)
          verifyGoalConsistency(foundGoal3, createdGoalData1)
        }
        response.pageable!!.also {
          assertThat(it.page).isEqualTo(0)
          assertThat(it.count).isEqualTo(3)
          assertThat(it.totalPages).isEqualTo(1)
          assertThat(it.totalElements).isEqualTo(3)
        }
      }

    @Test
    fun `목표 목록을 상태로 필터링할 수 있다`() =
      runTest {
        // given
        val activeGoal =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "title1",
              targetDate = Instant.now(),
            ),
          )
        val inProgressGoal =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "title1",
              targetDate = Instant.now(),
              status = GoalStatus.INPROGRESS,
            ),
          )
        val completedGoal =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "title1",
              targetDate = Instant.now(),
              status = GoalStatus.COMPLETED,
            ),
          )

        // when
        val response1 = getGoals("/api/v1/goals?status=READY")
        val response2 = getGoals("/api/v1/goals?status=INPROGRESS")
        val response3 = getGoals("/api/v1/goals?status=COMPLETED")

        // then
        assertGoalsAndPagination(response1, activeGoal)
        assertGoalsAndPagination(response2, inProgressGoal)
        assertGoalsAndPagination(response3, completedGoal)
      }

    @Test
    fun `목표 목록을 페이징으로 조회할 수 있다`() =
      runTest {
        // given
        val (_, createdGoal1) = postCreateGoal()
        val (_, createdGoal2) = postCreateGoal()
        val (_, createdGoal3) = postCreateGoal()

        // when
        val response1 = getGoals("/api/v1/goals?page=0&size=1")
        val response2 = getGoals("/api/v1/goals?page=1&size=1")
        val response3 = getGoals("/api/v1/goals?page=2&size=1")

        // then
        assertResponseGoalsAndPagination(response = response1, createdGoal = createdGoal3, page = 0)
        assertResponseGoalsAndPagination(response = response2, createdGoal = createdGoal2, page = 1)
        assertResponseGoalsAndPagination(response = response3, createdGoal = createdGoal1, page = 2)
      }

    @Test
    fun `Bearer token으로 잘못된 날짜 형식 요청 시 문제 상황 재현`() =
      runTest {
        // given - 유효한 Bearer token을 생성
        val validAccessToken = tokenService.createAccessToken(testUser)
        val request =
          mapOf(
            "title" to "Test Goal",
            "description" to "Test Description",
            // 잘못된 날짜 형식 (문자열)
            "targetDate" to "2025-12-31",
          )

        // when - 유효한 Bearer token으로 잘못된 JSON 데이터 요청
        val response =
          webTestClient
            .post()
            .uri("/api/v1/goals")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $validAccessToken")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .is4xxClientError // 400 또는 401 중 어떤 것이 나오는지 확인
            .expectBody<ServiceResponse<Unit>>()
            .returnResult()
            .responseBody!!

        // then
        assertThat(response.statusCode).isEqualTo(400)
      }

    private fun assertResponseGoalsAndPagination(
      response: ServiceResponse<List<GoalResponse>>,
      createdGoal: ServiceResponse<GoalResponse>,
      page: Int,
    ) {
      response.data!!.also {
        assertThat(it).hasSize(1)
        val createdGoalData = createdGoal.data!!
        val foundGoal = it[0]
        verifyGoalConsistency(foundGoal, createdGoalData)
      }
      response.pageable!!.also {
        assertThat(it.page).isEqualTo(page)
        assertThat(it.count).isEqualTo(1)
        assertThat(it.totalPages).isEqualTo(3)
        assertThat(it.totalElements).isEqualTo(3)
      }
    }

    private fun assertGoalsAndPagination(
      response: ServiceResponse<List<GoalResponse>>,
      goalEntity: GoalEntity,
    ) {
      response.also {
        it.data!!.also { goals ->
          val savedEntity = GoalResponse.from(Goal.from(goalEntity))
          assertThat(goals).hasSize(1)
          verifyGoalConsistency(goals[0], savedEntity)
        }
        it.pageable!!.also { pageable ->
          assertThat(pageable.page).isEqualTo(0)
          assertThat(pageable.count).isEqualTo(1)
          assertThat(pageable.totalPages).isEqualTo(1)
          assertThat(pageable.totalElements).isEqualTo(1)
        }
      }
    }

    private fun verifyGoalConsistency(
      actual: GoalResponse,
      expected: GoalResponse,
    ) {
      assertThat(actual.id).isEqualTo(expected.id)
      assertThat(actual.title).isEqualTo(expected.title)
      assertThat(actual.userId).isEqualTo(expected.userId)
      assertThat(actual.status).isEqualTo(expected.status)
      assertThat(actual.targetDate.truncatedTo(MILLIS))
        .isEqualTo(expected.targetDate.truncatedTo(MILLIS))
      assertThat(actual.imagePath).isEqualTo(expected.imagePath)
    }

    private fun getGoals(uri: String): ServiceResponse<List<GoalResponse>> =
      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .get()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ServiceResponse<List<GoalResponse>>>()
        .returnResult()
        .responseBody!!

    private fun postCreateGoal(): Pair<CreateGoalRequest, ServiceResponse<GoalResponse>> {
      val request =
        CreateGoalRequest(
          title = "Learn Kotlin",
          description = "Master Kotlin programming language by reading documentation and building projects",
          targetDate = Instant.now().plusSeconds(86400 * 30),
        )

      // when
      val response =
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/goals")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<GoalResponse>>()
          .returnResult()
          .responseBody!!
      return Pair(request, response)
    }

    @Test
    fun `목표의 모든 필드를 정상적으로 업데이트할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val request =
          UpdateGoalRequest(
            title = "Updated Title",
            description = "Updated Description",
            targetDate = Instant.now().plusSeconds(86400 * 60),
            status = GoalStatus.INPROGRESS,
          )

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          verifyGoalUpdate(testGoal, updated, request)
        }
      }

    @Test
    fun `목표 제목만 업데이트할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val request = UpdateGoalRequest(title = "Updated Title Only")

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          verifyGoalUpdate(testGoal, updated, request)
        }
      }

    @Test
    fun `목표 설명만 업데이트할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val request = UpdateGoalRequest(description = "Updated Description Only")

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          verifyGoalUpdate(testGoal, updated, request)
        }
      }

    @Test
    fun `목표 날짜만 업데이트할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val request = UpdateGoalRequest(targetDate = Instant.now().plusSeconds(86400 * 90))

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          verifyGoalUpdate(testGoal, updated, request)
        }
      }

    @Test
    fun `목표 상태만 업데이트할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val request = UpdateGoalRequest(status = GoalStatus.COMPLETED)

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          verifyGoalUpdate(testGoal, updated, request)
        }
      }

    @Test
    fun `이미지 ID를 null로 업데이트할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val request = UpdateGoalRequest(imageId = null)

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          verifyGoalUpdate(testGoal, updated, request)
          // imageId가 null로 업데이트되면 imagePath도 null이어야 함
          assertThat(updated.imagePath).isNull()
        }
      }

    @Test
    fun `목표 상태를 READY에서 INPROGRESS로 변경할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val request = UpdateGoalRequest(status = GoalStatus.INPROGRESS)

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          assertThat(updated.status).isEqualTo(GoalStatus.INPROGRESS)
        }
      }

    @Test
    fun `목표 상태를 INPROGRESS에서 COMPLETED로 변경할 수 있다`() =
      runTest {
        // given - INPROGRESS 상태의 목표 생성
        val inProgressGoal =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "In Progress Goal",
              targetDate = Instant.now().plusSeconds(86400),
              status = GoalStatus.INPROGRESS,
            ),
          )
        val request = UpdateGoalRequest(status = GoalStatus.COMPLETED)

        // when
        val response = putUpdateGoal(inProgressGoal.id!!, request)

        // then
        response.data?.also { updated ->
          assertThat(updated.status).isEqualTo(GoalStatus.COMPLETED)
        }
      }

    @Test
    fun `목표 상태를 COMPLETED에서 READY로 되돌릴 수 있다`() =
      runTest {
        // given - COMPLETED 상태의 목표 생성
        val completedGoal =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Completed Goal",
              targetDate = Instant.now().plusSeconds(86400),
              status = GoalStatus.COMPLETED,
            ),
          )
        val request = UpdateGoalRequest(status = GoalStatus.READY)

        // when
        val response = putUpdateGoal(completedGoal.id!!, request)

        // then
        response.data?.also { updated ->
          assertThat(updated.status).isEqualTo(GoalStatus.READY)
        }
      }

    @Test
    fun `모든 필드가 null인 요청으로도 업데이트할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val request = UpdateGoalRequest()

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          // 모든 필드는 기존값 유지, updatedAt만 갱신
          assertThat(updated.title).isEqualTo(testGoal.title)
          assertThat(updated.description).isEqualTo(testGoal.description)
          assertThat(updated.targetDate.truncatedTo(MILLIS))
            .isEqualTo(testGoal.targetDate.truncatedTo(MILLIS))
          assertThat(updated.imagePath).isEqualTo(testGoal.imagePath)
          assertThat(updated.status).isEqualTo(testGoal.status)
          assertThat(updated.updatedAt).isAfter(testGoal.updatedAt)
        }
      }

    @Test
    fun `존재하지 않는 목표 ID로 업데이트 시도 시 404 반환`() {
      // given
      val nonExistentGoalId = UUID.randomUUID()
      val request = UpdateGoalRequest(title = "Updated Title")

      // when
      val response = putUpdateGoalExpectingError(nonExistentGoalId, request)

      // then
      verifyErrorResponse(response, 404, "GOAL:GOALNOTFOUND")
    }

    @Test
    fun `다른 사용자의 목표를 업데이트 시도 시 403 반환`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val request = UpdateGoalRequest(title = "Hacked Title")
        val otherUserAuth =
          UsernamePasswordAuthenticationToken(
            otherUser,
            null,
            listOf(SimpleGrantedAuthority(Role.USER.name)),
          )

        // when
        val response = putUpdateGoalExpectingError(testGoal.id, request, otherUserAuth)

        // then
        verifyErrorResponse(response, 403, "GOAL:ACCESSDENIED")
      }

    @Test
    fun `인증되지 않은 사용자의 업데이트 시도 시 401 반환`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val request = UpdateGoalRequest(title = "Unauthorized Update")

        // when
        val response = putUpdateGoalExpectingError(testGoal.id, request, null)

        // then
        verifyErrorResponse(response, 401, "AUTH:UNAUTHORIZED")
      }

    @Test
    fun `잘못된 UUID 형식의 goalId로 요청 시 400 반환`() {
      // given
      val request = UpdateGoalRequest(title = "Valid Title")

      // when & then
      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .put()
        .uri("/api/v1/goals/invalid-uuid-format")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `잘못된 날짜 형식으로 업데이트 시도 시 400 반환`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val invalidRequest =
          mapOf(
            "title" to "Valid Title",
            "targetDate" to "invalid-date-format",
          )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .put()
          .uri("/api/v1/goals/${testGoal.id}")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(invalidRequest)
          .exchange()
          .expectStatus()
          .isBadRequest
      }

    @Test
    fun `존재하지 않는 GoalStatus 값으로 업데이트 시도 시 400 반환`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val invalidRequest =
          mapOf(
            "title" to "Valid Title",
            "status" to "INVALID_STATUS",
          )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .put()
          .uri("/api/v1/goals/${testGoal.id}")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(invalidRequest)
          .exchange()
          .expectStatus()
          .isBadRequest
      }

    @Test
    fun `잘못된 JSON 형식으로 요청 시 400 반환`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val invalidJson = "{ invalid json }"

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .put()
          .uri("/api/v1/goals/${testGoal.id}")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(invalidJson)
          .exchange()
          .expectStatus()
          .isBadRequest
      }

    @Test
    fun `삭제된 목표를 업데이트 시도 시 404 반환`() =
      runTest {
        // given - 삭제된 목표 생성
        val deletedGoal =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Deleted Goal",
              targetDate = Instant.now().plusSeconds(86400),
              deletedAt = Instant.now(),
            ),
          )
        val request = UpdateGoalRequest(title = "Updated Title")

        // when
        val response = putUpdateGoalExpectingError(deletedGoal.id!!, request)

        // then
        verifyErrorResponse(response, 404, "GOAL:GOALNOTFOUND")
      }

    @Test
    fun `긴 제목으로 업데이트할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val longTitle = "A".repeat(255) // 255자로 제한하여 테스트
        val request = UpdateGoalRequest(title = longTitle)

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          assertThat(updated.title).isEqualTo(longTitle)
        }
      }

    @Test
    fun `매우 긴 설명으로 업데이트할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val longDescription = "B".repeat(2000)
        val request = UpdateGoalRequest(description = longDescription)

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          assertThat(updated.description).isEqualTo(longDescription)
        }
      }

    @Test
    fun `과거 날짜로 목표 날짜를 업데이트할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val pastDate = Instant.now().minusSeconds(86400 * 30)
        val request = UpdateGoalRequest(targetDate = pastDate)

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          assertThat(updated.targetDate.truncatedTo(MILLIS))
            .isEqualTo(pastDate.truncatedTo(MILLIS))
        }
      }

    @Test
    fun `먼 미래 날짜로 목표 날짜를 업데이트할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val futureDate = Instant.now().plusSeconds(86400 * 365 * 10) // 10년 후
        val request = UpdateGoalRequest(targetDate = futureDate)

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          assertThat(updated.targetDate.truncatedTo(MILLIS))
            .isEqualTo(futureDate.truncatedTo(MILLIS))
        }
      }

    @Test
    fun `특수문자가 포함된 제목으로 업데이트할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val specialCharTitle = "!@#\$%^&*()_+-=[]{}|;':\",./<>?"
        val request = UpdateGoalRequest(title = specialCharTitle)

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          assertThat(updated.title).isEqualTo(specialCharTitle)
        }
      }

    @Test
    fun `이모지가 포함된 제목으로 업데이트할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val emojiTitle = "🎯 달성해야 할 목표 🚀 화이팅! 💪"
        val request = UpdateGoalRequest(title = emojiTitle)

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          assertThat(updated.title).isEqualTo(emojiTitle)
        }
      }

    @Test
    fun `다국어 텍스트로 업데이트할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val multilingualTitle = "English 한국어 日本語 中文 العربية Русский"
        val request = UpdateGoalRequest(title = multilingualTitle)

        // when
        val response = putUpdateGoal(testGoal.id, request)

        // then
        response.data?.also { updated ->
          assertThat(updated.title).isEqualTo(multilingualTitle)
        }
      }

    @Test
    fun `목표를 정상적으로 삭제할 수 있다`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)

        // when
        val response = deleteGoal(testGoal.id)

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isEqualTo("Goal deleted successfully")

        // 실제로 데이터베이스에서 삭제되었는지 확인 (soft delete)
        val deletedGoal = goalRepository.findById(testGoal.id)
        assertThat(deletedGoal).isNotNull()
        assertThat(deletedGoal!!.deletedAt).isNotNull()
      }

    @Test
    fun `인증되지 않은 사용자의 삭제 시도 시 401 반환`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)

        // when
        val response = deleteGoalExpectingError(testGoal.id, null)

        // then
        verifyErrorResponse(response, 401, "AUTH:UNAUTHORIZED")
      }

    @Test
    fun `다른 사용자의 목표 삭제 시도 시 403 반환`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val otherUserAuth =
          UsernamePasswordAuthenticationToken(
            otherUser,
            null,
            listOf(SimpleGrantedAuthority(Role.USER.name)),
          )

        // when
        val response = deleteGoalExpectingError(testGoal.id, otherUserAuth)

        // then
        verifyErrorResponse(response, 403, "GOAL:ACCESSDENIED")
      }

    @Test
    fun `존재하지 않는 목표 ID로 삭제 시도 시 404 반환`() {
      // given
      val nonExistentGoalId = UUID.randomUUID()

      // when
      val response = deleteGoalExpectingError(nonExistentGoalId)

      // then
      verifyErrorResponse(response, 404, "GOAL:GOALNOTFOUND")
    }

    @Test
    fun `deleteGoal 시 잘못된 UUID 형식 요청 시 400 반환`() {
      // when & then
      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .delete()
        .uri("/api/v1/goals/invalid-uuid-format")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `이미 삭제된 목표를 삭제 시도 시 404 반환`() =
      runTest {
        // given - 삭제된 목표 생성
        val deletedGoal =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Deleted Goal",
              targetDate = Instant.now().plusSeconds(86400),
              deletedAt = Instant.now(),
            ),
          )

        // when
        val response = deleteGoalExpectingError(deletedGoal.id!!)

        // then
        verifyErrorResponse(response, 404, "GOAL:GOALNOTFOUND")
      }

    private suspend fun createTestGoal(user: User): GoalResponse {
      val goalEntity =
        goalRepository.save(
          GoalEntity(
            userId = user.id,
            title = "Original Title",
            description = "Original Description",
            targetDate = Instant.now().plusSeconds(86400 * 30),
          ),
        )
      return GoalResponse.from(Goal.from(goalEntity))
    }

    private fun putUpdateGoal(
      goalId: UUID,
      request: UpdateGoalRequest,
      auth: UsernamePasswordAuthenticationToken = authentication,
    ): ServiceResponse<GoalResponse> =
      webTestClient
        .mutateWith(mockAuthentication(auth))
        .put()
        .uri("/api/v1/goals/$goalId")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ServiceResponse<GoalResponse>>()
        .returnResult()
        .responseBody!!

    private fun putUpdateGoalExpectingError(
      goalId: UUID,
      request: UpdateGoalRequest,
      auth: UsernamePasswordAuthenticationToken? = authentication,
    ): ServiceResponse<Unit> {
      val testClient =
        if (auth != null) {
          webTestClient.mutateWith(mockAuthentication(auth))
        } else {
          webTestClient
        }

      return testClient
        .put()
        .uri("/api/v1/goals/$goalId")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody<ServiceResponse<Unit>>()
        .returnResult()
        .responseBody!!
    }

    private fun verifyGoalUpdate(
      original: GoalResponse,
      updated: GoalResponse,
      request: UpdateGoalRequest,
    ) {
      assertThat(updated.id).isEqualTo(original.id)
      assertThat(updated.userId).isEqualTo(original.userId)
      assertThat(updated.title).isEqualTo(request.title ?: original.title)
      assertThat(updated.description).isEqualTo(request.description ?: original.description)

      val requestTargetDate = request.targetDate
      if (requestTargetDate != null) {
        assertThat(updated.targetDate.truncatedTo(MILLIS))
          .isEqualTo(requestTargetDate.truncatedTo(MILLIS))
      } else {
        assertThat(updated.targetDate.truncatedTo(MILLIS))
          .isEqualTo(original.targetDate.truncatedTo(MILLIS))
      }

      // imagePath는 imageId 업데이트 시 변경될 수 있지만, 직접 비교는 어려우므로
      // imageId가 요청에 포함된 경우 imagePath가 변경되었는지만 확인
      if (request.imageId != null) {
        // imageId가 업데이트되었다면 imagePath도 변경되거나 null이어야 함
        // 실제 이미지 파일이 없을 수 있으므로 null도 허용
      } else {
        assertThat(updated.imagePath).isEqualTo(original.imagePath)
      }

      assertThat(updated.status).isEqualTo(request.status ?: original.status)
      assertThat(updated.updatedAt).isAfter(original.updatedAt)
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

    private fun deleteGoal(
      goalId: UUID,
      auth: UsernamePasswordAuthenticationToken = authentication,
    ): ServiceResponse<String> =
      webTestClient
        .mutateWith(mockAuthentication(auth))
        .delete()
        .uri("/api/v1/goals/$goalId")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ServiceResponse<String>>()
        .returnResult()
        .responseBody!!

    private fun deleteGoalExpectingError(
      goalId: UUID,
      auth: UsernamePasswordAuthenticationToken? = authentication,
    ): ServiceResponse<Unit> {
      val testClient =
        if (auth != null) {
          webTestClient.mutateWith(mockAuthentication(auth))
        } else {
          webTestClient
        }

      return testClient
        .delete()
        .uri("/api/v1/goals/$goalId")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody<ServiceResponse<Unit>>()
        .returnResult()
        .responseBody!!
    }

    @Test
    fun `정상적인 목표 상세 조회 테스트`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)

        // when
        val response = getGoalDetail(testGoal.id)

        // then
        response.data?.also { goalDetail ->
          assertThat(goalDetail.id).isEqualTo(testGoal.id)
          assertThat(goalDetail.title).isEqualTo(testGoal.title)
          assertThat(goalDetail.description).isEqualTo(testGoal.description)
          assertThat(goalDetail.userId).isEqualTo(testGoal.userId)
          assertThat(goalDetail.status).isEqualTo(testGoal.status)
          assertThat(goalDetail.targetDate.truncatedTo(MILLIS))
            .isEqualTo(testGoal.targetDate.truncatedTo(MILLIS))
          assertThat(goalDetail.imagePath).isEqualTo(testGoal.imagePath)
        }
      }

    @Test
    fun `인증되지 않은 사용자 요청 시 401 반환 - getGoalDetail`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)

        // when
        val response = getGoalDetailExpectingError(testGoal.id, null)

        // then
        verifyErrorResponse(response, 401, "AUTH:UNAUTHORIZED")
      }

    @Test
    fun `다른 사용자의 목표 조회 시도 시 403 반환`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val otherUserAuth =
          UsernamePasswordAuthenticationToken(
            otherUser,
            null,
            listOf(SimpleGrantedAuthority(Role.USER.name)),
          )

        // when
        val response = getGoalDetailExpectingError(testGoal.id, otherUserAuth)

        // then
        verifyErrorResponse(response, 403, "GOAL:ACCESSDENIED")
      }

    @Test
    fun `존재하지 않는 목표 ID로 조회 시 404 반환`() {
      // given
      val nonExistentGoalId = UUID.randomUUID()

      // when
      val response = getGoalDetailExpectingError(nonExistentGoalId)

      // then
      verifyErrorResponse(response, 404, "GOAL:GOALNOTFOUND")
    }

    @Test
    fun `잘못된 UUID 형식 요청 시 400 반환 - getGoalDetail`() {
      // when & then
      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .get()
        .uri("/api/v1/goals/invalid-uuid-format")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `삭제된 목표 조회 시도 시 404 반환`() =
      runTest {
        // given - 삭제된 목표 생성
        val deletedGoal =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Deleted Goal",
              targetDate = Instant.now().plusSeconds(86400),
              deletedAt = Instant.now(),
            ),
          )

        // when
        val response = getGoalDetailExpectingError(deletedGoal.id!!)

        // then
        verifyErrorResponse(response, 404, "GOAL:GOALNOTFOUND")
      }

    @Test
    fun `Bearer token으로 목표 상세 조회 테스트`() =
      runTest {
        // given
        val testGoal = createTestGoal(testUser)
        val validAccessToken = tokenService.createAccessToken(testUser)

        // when
        val response =
          webTestClient
            .get()
            .uri("/api/v1/goals/${testGoal.id}")
            .accept(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer $validAccessToken")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<GoalResponse>>()
            .returnResult()
            .responseBody!!

        // then
        response.data?.also { goalDetail ->
          assertThat(goalDetail.id).isEqualTo(testGoal.id)
          assertThat(goalDetail.title).isEqualTo(testGoal.title)
          assertThat(goalDetail.userId).isEqualTo(testGoal.userId)
        }
      }

    @Test
    fun `존재하지 않는 사용자로 목표 조회 시 적절한 에러 처리 - getGoalDetail`() {
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
      val anyGoalId = UUID.randomUUID()

      // when
      val response =
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/$anyGoalId")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody<ServiceResponse<Unit>>()
          .returnResult()
          .responseBody!!

      // then
      response.also {
        assertThat(it.statusCode).isEqualTo(404)
        assertThat(it.errorCode).isEqualTo("GOAL:GOALNOTFOUND")
      }
    }

    private fun getGoalDetail(
      goalId: UUID,
      auth: UsernamePasswordAuthenticationToken = authentication,
    ): ServiceResponse<GoalResponse> =
      webTestClient
        .mutateWith(mockAuthentication(auth))
        .get()
        .uri("/api/v1/goals/$goalId")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ServiceResponse<GoalResponse>>()
        .returnResult()
        .responseBody!!

    private fun getGoalDetailExpectingError(
      goalId: UUID,
      auth: UsernamePasswordAuthenticationToken? = authentication,
    ): ServiceResponse<Unit> {
      val testClient =
        if (auth != null) {
          webTestClient.mutateWith(mockAuthentication(auth))
        } else {
          webTestClient
        }

      return testClient
        .get()
        .uri("/api/v1/goals/$goalId")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody<ServiceResponse<Unit>>()
        .returnResult()
        .responseBody!!
    }

    @Test
    fun `목표 목록 조회 시 일정 통계가 포함된다`() =
      runTest {
        // given
        val goalEntity =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Test Goal with Schedules",
              targetDate = Instant.now().plusSeconds(86400),
            ),
          )

        // 일정 5개 생성: 2개 완료, 3개 미완료
        val now = Instant.now()
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalEntity.id!!,
            userId = testUser.id,
            title = "Schedule 1",
            startDate = now,
            endDate = now.plusSeconds(3600),
            status = ScheduleStatus.COMPLETED,
          ),
        )
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalEntity.id!!,
            userId = testUser.id,
            title = "Schedule 2",
            startDate = now,
            endDate = now.plusSeconds(3600),
            status = ScheduleStatus.COMPLETED,
          ),
        )
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalEntity.id!!,
            userId = testUser.id,
            title = "Schedule 3",
            startDate = now,
            endDate = now.plusSeconds(3600),
            status = ScheduleStatus.READY,
          ),
        )
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalEntity.id!!,
            userId = testUser.id,
            title = "Schedule 4",
            startDate = now,
            endDate = now.plusSeconds(3600),
            status = ScheduleStatus.READY,
          ),
        )
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalEntity.id!!,
            userId = testUser.id,
            title = "Schedule 5",
            startDate = now,
            endDate = now.plusSeconds(3600),
            status = ScheduleStatus.READY,
          ),
        )

        // when
        val response = getGoals("/api/v1/goals")

        // then
        response.data!!.also { goals ->
          assertThat(goals).hasSize(1)
          val goal = goals[0]
          assertThat(goal.totalScheduleCount).isEqualTo(5)
          assertThat(goal.completedScheduleCount).isEqualTo(2)
        }
      }

    @Test
    fun `일정이 없는 목표의 경우 일정 통계가 0으로 반환된다`() =
      runTest {
        // given
        goalRepository.save(
          GoalEntity(
            userId = testUser.id,
            title = "Goal without Schedules",
            targetDate = Instant.now().plusSeconds(86400),
          ),
        )

        // when
        val response = getGoals("/api/v1/goals")

        // then
        response.data!!.also { goals ->
          assertThat(goals).hasSize(1)
          val goal = goals[0]
          assertThat(goal.totalScheduleCount).isEqualTo(0)
          assertThat(goal.completedScheduleCount).isEqualTo(0)
        }
      }

    @Test
    fun `삭제된 일정은 통계에서 제외된다`() =
      runTest {
        // given
        val goalEntity =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Test Goal with Deleted Schedule",
              targetDate = Instant.now().plusSeconds(86400),
            ),
          )

        val now = Instant.now()
        // 삭제되지 않은 일정 2개
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalEntity.id!!,
            userId = testUser.id,
            title = "Active Schedule 1",
            startDate = now,
            endDate = now.plusSeconds(3600),
            status = ScheduleStatus.COMPLETED,
          ),
        )
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalEntity.id!!,
            userId = testUser.id,
            title = "Active Schedule 2",
            startDate = now,
            endDate = now.plusSeconds(3600),
            status = ScheduleStatus.READY,
          ),
        )
        // 삭제된 일정 1개
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalEntity.id!!,
            userId = testUser.id,
            title = "Deleted Schedule",
            startDate = now,
            endDate = now.plusSeconds(3600),
            status = ScheduleStatus.COMPLETED,
            deletedAt = now,
          ),
        )

        // when
        val response = getGoals("/api/v1/goals")

        // then
        response.data!!.also { goals ->
          assertThat(goals).hasSize(1)
          val goal = goals[0]
          assertThat(goal.totalScheduleCount).isEqualTo(2)
          assertThat(goal.completedScheduleCount).isEqualTo(1)
        }
      }

    @Test
    fun `여러 목표가 있을 때 각 목표별로 정확한 일정 통계가 반환된다`() =
      runTest {
        // given
        val goalEntity1 =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Goal 1",
              targetDate = Instant.now().plusSeconds(86400),
            ),
          )
        val goalEntity2 =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Goal 2",
              targetDate = Instant.now().plusSeconds(86400),
            ),
          )

        val now = Instant.now()
        // Goal 1에 일정 3개: 1개 완료, 2개 미완료
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalEntity1.id!!,
            userId = testUser.id,
            title = "G1 Schedule 1",
            startDate = now,
            endDate = now.plusSeconds(3600),
            status = ScheduleStatus.COMPLETED,
          ),
        )
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalEntity1.id!!,
            userId = testUser.id,
            title = "G1 Schedule 2",
            startDate = now,
            endDate = now.plusSeconds(3600),
            status = ScheduleStatus.READY,
          ),
        )
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalEntity1.id!!,
            userId = testUser.id,
            title = "G1 Schedule 3",
            startDate = now,
            endDate = now.plusSeconds(3600),
            status = ScheduleStatus.READY,
          ),
        )

        // Goal 2에 일정 2개: 2개 완료, 0개 미완료
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalEntity2.id!!,
            userId = testUser.id,
            title = "G2 Schedule 1",
            startDate = now,
            endDate = now.plusSeconds(3600),
            status = ScheduleStatus.COMPLETED,
          ),
        )
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalEntity2.id!!,
            userId = testUser.id,
            title = "G2 Schedule 2",
            startDate = now,
            endDate = now.plusSeconds(3600),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        // when
        val response = getGoals("/api/v1/goals")

        // then
        response.data!!.also { goals ->
          assertThat(goals).hasSize(2)

          // 최신순으로 정렬되므로 goalEntity2가 먼저
          val goal2 = goals.find { it.id == goalEntity2.id }!!
          assertThat(goal2.totalScheduleCount).isEqualTo(2)
          assertThat(goal2.completedScheduleCount).isEqualTo(2)

          val goal1 = goals.find { it.id == goalEntity1.id }!!
          assertThat(goal1.totalScheduleCount).isEqualTo(3)
          assertThat(goal1.completedScheduleCount).isEqualTo(1)
        }
      }
  }
