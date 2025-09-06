package ai.aiminder.aiminderserver.goal.controller

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.domain.GoalStatus
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequest
import ai.aiminder.aiminderserver.goal.entity.GoalEntity
import ai.aiminder.aiminderserver.goal.repository.GoalRepository
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

class GoalControllerTest
  @Autowired
  constructor(
    private val userRepository: UserRepository,
    private val goalRepository: GoalRepository,
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
            status = GoalStatus.ARCHIVED,
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
          assertThat(foundGoal1).isEqualTo(createdGoalData3)
          assertThat(foundGoal2).isEqualTo(createdGoalData2)
          assertThat(foundGoal3).isEqualTo(createdGoalData1)
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
        val archivedGoal =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "title1",
              targetDate = Instant.now(),
              status = GoalStatus.ARCHIVED,
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
        val response1 = getGoals("/api/v1/goals?status=ACTIVE")
        val response2 = getGoals("/api/v1/goals?status=ARCHIVED")
        val response3 = getGoals("/api/v1/goals?status=COMPLETED")

        // then
        assertGoalsAndPagination(response1, activeGoal)
        assertGoalsAndPagination(response2, archivedGoal)
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

    private fun assertResponseGoalsAndPagination(
      response: ServiceResponse<List<Goal>>,
      createdGoal: ServiceResponse<Goal>,
      page: Int,
    ) {
      response.data!!.also {
        assertThat(it).hasSize(1)
        val createdGoalData = createdGoal.data!!
        val foundGoal = it[0]
        assertThat(foundGoal).isEqualTo(createdGoalData)
      }
      response.pageable!!.also {
        assertThat(it.page).isEqualTo(page)
        assertThat(it.count).isEqualTo(1)
        assertThat(it.totalPages).isEqualTo(3)
        assertThat(it.totalElements).isEqualTo(3)
      }
    }

    private fun assertGoalsAndPagination(
      response: ServiceResponse<List<Goal>>,
      goalEntity: GoalEntity,
    ) {
      response.also {
        it.data!!.also { goals ->
          assertThat(goals).hasSize(1)
          assertThat(goals[0]).isEqualTo(Goal.from(goalEntity))
        }
        it.pageable!!.also { pageable ->
          assertThat(pageable.page).isEqualTo(0)
          assertThat(pageable.count).isEqualTo(1)
          assertThat(pageable.totalPages).isEqualTo(1)
          assertThat(pageable.totalElements).isEqualTo(1)
        }
      }
    }

    private fun getGoals(uri: String): ServiceResponse<List<Goal>> =
      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .get()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ServiceResponse<List<Goal>>>()
        .returnResult()
        .responseBody!!

    private fun postCreateGoal(): Pair<CreateGoalRequest, ServiceResponse<Goal>> {
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
          .expectBody<ServiceResponse<Goal>>()
          .returnResult()
          .responseBody!!
      return Pair(request, response)
    }
  }
