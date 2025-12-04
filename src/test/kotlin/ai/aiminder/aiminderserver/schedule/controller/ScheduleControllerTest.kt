package ai.aiminder.aiminderserver.schedule.controller

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.goal.domain.GoalStatus
import ai.aiminder.aiminderserver.goal.entity.GoalEntity
import ai.aiminder.aiminderserver.goal.repository.GoalRepository
import ai.aiminder.aiminderserver.schedule.domain.ScheduleStatus
import ai.aiminder.aiminderserver.schedule.dto.CreateScheduleRequest
import ai.aiminder.aiminderserver.schedule.dto.MonthlyScheduleStatisticsResponse
import ai.aiminder.aiminderserver.schedule.dto.ScheduleResponse
import ai.aiminder.aiminderserver.schedule.dto.UpdateScheduleRequest
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
import java.util.UUID

class ScheduleControllerTest
  @Autowired
  constructor(
    private val userRepository: UserRepository,
    private val goalRepository: GoalRepository,
    private val scheduleRepository: ScheduleRepository,
  ) : BaseIntegrationTest() {
    private lateinit var testUser: User
    private lateinit var testGoal: GoalEntity
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

        testGoal =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Test Goal",
              description = "Test Goal Description",
              targetDate = Instant.parse("2024-04-15T00:00:00Z"),
              status = GoalStatus.READY,
            ),
          )

        authentication =
          UsernamePasswordAuthenticationToken(
            testUser,
            null,
            listOf(SimpleGrantedAuthority(Role.USER.name)),
          )
      }

    @Test
    fun `일정 생성 성공`() =
      runTest {
        // given
        val request =
          CreateScheduleRequest(
            title = "Test Schedule",
            description = "Test Schedule Content",
            startDate = Instant.parse("2024-03-15T00:00:00Z"),
            endDate = Instant.parse("2024-03-22T00:00:00Z"),
          )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/goals/{goalId}/schedules", testGoal.id!!)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<ScheduleResponse>>()
          .value { response ->
            assertThat(response.data?.title).isEqualTo("Test Schedule")
            assertThat(response.data?.description).isEqualTo("Test Schedule Content")
            assertThat(response.data?.goalId).isEqualTo(testGoal.id)
            assertThat(response.data?.userId).isEqualTo(testUser.id)
            assertThat(response.data?.status).isEqualTo(ScheduleStatus.READY)
            assertThat(response.data?.startDate).isEqualTo(Instant.parse("2024-03-15T00:00:00Z"))
            assertThat(response.data?.endDate).isEqualTo(Instant.parse("2024-03-22T00:00:00Z"))
          }
      }

    @Test
    fun `일정 목록 조회 성공`() =
      runTest {
        // given
        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "Schedule 1",
            description = "Content 1",
            startDate = Instant.parse("2024-03-15T00:00:00Z"),
            endDate = Instant.parse("2024-03-20T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "Schedule 2",
            description = "Content 2",
            startDate = Instant.parse("2024-03-21T00:00:00Z"),
            endDate = Instant.parse("2024-03-25T00:00:00Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(2)
            assertThat(response.data?.map { it.title }).containsExactlyInAnyOrder("Schedule 1", "Schedule 2")
          }
      }

    @Test
    fun `일정 수정 성공`() =
      runTest {
        // given
        val schedule =
          scheduleRepository.save(
            ScheduleEntity(
              goalId = testGoal.id!!,
              userId = testUser.id,
              title = "Original Title",
              description = "Original Content",
              startDate = Instant.parse("2024-03-21T00:00:00Z"),
              endDate = Instant.parse("2024-03-25T00:00:00Z"),
              status = ScheduleStatus.READY,
            ),
          )

        val updateRequest =
          UpdateScheduleRequest(
            title = "Updated Title",
            description = "Updated Content",
            status = ScheduleStatus.COMPLETED,
          )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .put()
          .uri("/api/v1/goals/schedules/${schedule.id}", testGoal.id!!)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(updateRequest)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<ScheduleResponse>>()
          .value { response ->
            assertThat(response.data?.title).isEqualTo("Updated Title")
            assertThat(response.data?.description).isEqualTo("Updated Content")
            assertThat(response.data?.status).isEqualTo(ScheduleStatus.COMPLETED)
          }
      }

    @Test
    fun `일정 삭제 성공`() =
      runTest {
        // given
        val schedule =
          scheduleRepository.save(
            ScheduleEntity(
              goalId = testGoal.id!!,
              userId = testUser.id,
              title = "Schedule to Delete",
              description = "Content",
              startDate = Instant.parse("2024-03-15T00:00:00Z"),
              endDate = Instant.parse("2024-03-20T00:00:00Z"),
              status = ScheduleStatus.READY,
            ),
          )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .delete()
          .uri("/api/v1/goals/schedules/${schedule.id}", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<String>>()
          .value { response ->
            assertThat(response.data).isEqualTo("Schedule deleted successfully")
          }
      }

    @Test
    fun `일정 상세 조회 성공`() =
      runTest {
        // given
        val schedule =
          scheduleRepository.save(
            ScheduleEntity(
              goalId = testGoal.id!!,
              userId = testUser.id,
              title = "Schedule Detail",
              description = "Detail Content",
              startDate = Instant.parse("2024-03-15T00:00:00Z"),
              endDate = Instant.parse("2024-03-20T00:00:00Z"),
              status = ScheduleStatus.READY,
            ),
          )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/schedules/${schedule.id}", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<ScheduleResponse>>()
          .value { response ->
            assertThat(response.data?.title).isEqualTo("Schedule Detail")
            assertThat(response.data?.description).isEqualTo("Detail Content")
            assertThat(response.data?.id).isEqualTo(schedule.id)
          }
      }

    @Test
    fun `존재하지 않는 일정 조회 시 404 반환`() =
      runTest {
        // given
        val nonExistentId = UUID.randomUUID()

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules/$nonExistentId", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isNotFound
      }

    @Test
    fun `인증되지 않은 사용자는 401 반환`() =
      runTest {
        // when & then
        webTestClient
          .get()
          .uri("/api/v1/goals/{goalId}/schedules", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isUnauthorized
      }

    @Test
    fun `goalId로 스케줄 필터링 성공`() =
      runTest {
        // given
        val anotherGoal =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Another Goal",
              description = "Another Goal Description",
              targetDate = Instant.parse("2024-05-15T00:00:00Z"),
              status = GoalStatus.READY,
            ),
          )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "Schedule for Test Goal",
            description = "Content 1",
            startDate = Instant.parse("2024-03-15T00:00:00Z"),
            endDate = Instant.parse("2024-03-20T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = anotherGoal.id!!,
            userId = testUser.id,
            title = "Schedule for Another Goal",
            description = "Content 2",
            startDate = Instant.parse("2024-03-21T00:00:00Z"),
            endDate = Instant.parse("2024-03-25T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        // when & then - Filter by testGoal
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.title).isEqualTo("Schedule for Test Goal")
            assertThat(response.data?.first()?.goalId).isEqualTo(testGoal.id)
          }

        // when & then - Filter by anotherGoal
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules", anotherGoal.id)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.title).isEqualTo("Schedule for Another Goal")
            assertThat(response.data?.first()?.goalId).isEqualTo(anotherGoal.id)
          }
      }

    @Test
    fun `status로 스케줄 필터링 성공`() =
      runTest {
        // given
        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "Ready Schedule",
            description = "Ready Content",
            startDate = Instant.parse("2024-03-15T00:00:00Z"),
            endDate = Instant.parse("2024-03-20T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "Completed Schedule",
            description = "Completed Content",
            startDate = Instant.parse("2024-03-21T00:00:00Z"),
            endDate = Instant.parse("2024-03-25T00:00:00Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        // when & then - Filter by READY status
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules?status=READY", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.title).isEqualTo("Ready Schedule")
            assertThat(response.data?.first()?.status).isEqualTo(ScheduleStatus.READY)
          }

        // when & then - Filter by COMPLETED status
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules?status=COMPLETED", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.title).isEqualTo("Completed Schedule")
            assertThat(response.data?.first()?.status).isEqualTo(ScheduleStatus.COMPLETED)
          }
      }

    @Test
    fun `날짜 범위로 스케줄 필터링 성공`() =
      runTest {
        // given
        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "March Schedule",
            description = "March Content",
            startDate = Instant.parse("2024-03-15T00:00:00Z"),
            endDate = Instant.parse("2024-03-20T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "April Schedule",
            description = "April Content",
            startDate = Instant.parse("2024-04-10T00:00:00Z"),
            endDate = Instant.parse("2024-04-15T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "May Schedule",
            description = "May Content",
            startDate = Instant.parse("2024-05-10T00:00:00Z"),
            endDate = Instant.parse("2024-05-15T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        // when & then - Filter by March to April range
        val startDate = Instant.parse("2024-03-01T00:00:00Z")
        val endDate = Instant.parse("2024-04-30T23:59:59Z")

        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules?startDate=$startDate&endDate=$endDate", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(2)
            val titles = response.data?.map { it.title }
            assertThat(titles).containsExactlyInAnyOrder("March Schedule", "April Schedule")
          }
      }

    @Test
    fun `goalId와 status 조합 필터링 성공`() =
      runTest {
        // given
        val goalA =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Goal A",
              description = "Goal A Description",
              targetDate = Instant.parse("2024-04-15T00:00:00Z"),
              status = GoalStatus.READY,
            ),
          )

        val goalB =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Goal B",
              description = "Goal B Description",
              targetDate = Instant.parse("2024-05-15T00:00:00Z"),
              status = GoalStatus.READY,
            ),
          )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalA.id!!,
            userId = testUser.id,
            title = "Goal A - Ready Schedule",
            description = "Content",
            startDate = Instant.parse("2024-03-15T00:00:00Z"),
            endDate = Instant.parse("2024-03-20T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalA.id!!,
            userId = testUser.id,
            title = "Goal A - Completed Schedule",
            description = "Content",
            startDate = Instant.parse("2024-03-21T00:00:00Z"),
            endDate = Instant.parse("2024-03-25T00:00:00Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalB.id!!,
            userId = testUser.id,
            title = "Goal B - Ready Schedule",
            description = "Content",
            startDate = Instant.parse("2024-04-01T00:00:00Z"),
            endDate = Instant.parse("2024-04-05T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalB.id!!,
            userId = testUser.id,
            title = "Goal B - Completed Schedule",
            description = "Content",
            startDate = Instant.parse("2024-04-06T00:00:00Z"),
            endDate = Instant.parse("2024-04-10T00:00:00Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        // when & then - Filter by Goal A + READY status
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules?status=READY", goalA.id)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.title).isEqualTo("Goal A - Ready Schedule")
            assertThat(response.data?.first()?.goalId).isEqualTo(goalA.id)
            assertThat(response.data?.first()?.status).isEqualTo(ScheduleStatus.READY)
          }
      }

    @Test
    fun `goalId와 날짜 범위 조합 필터링 성공`() =
      runTest {
        // given
        val goalA =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Goal A",
              description = "Goal A Description",
              targetDate = Instant.parse("2024-04-15T00:00:00Z"),
              status = GoalStatus.READY,
            ),
          )

        val goalB =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Goal B",
              description = "Goal B Description",
              targetDate = Instant.parse("2024-05-15T00:00:00Z"),
              status = GoalStatus.READY,
            ),
          )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalA.id!!,
            userId = testUser.id,
            title = "Goal A - March Schedule",
            description = "Content",
            startDate = Instant.parse("2024-03-15T00:00:00Z"),
            endDate = Instant.parse("2024-03-20T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalA.id!!,
            userId = testUser.id,
            title = "Goal A - April Schedule",
            description = "Content",
            startDate = Instant.parse("2024-04-10T00:00:00Z"),
            endDate = Instant.parse("2024-04-15T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalB.id!!,
            userId = testUser.id,
            title = "Goal B - March Schedule",
            description = "Content",
            startDate = Instant.parse("2024-03-10T00:00:00Z"),
            endDate = Instant.parse("2024-03-15T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalB.id!!,
            userId = testUser.id,
            title = "Goal B - April Schedule",
            description = "Content",
            startDate = Instant.parse("2024-04-05T00:00:00Z"),
            endDate = Instant.parse("2024-04-10T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        // when & then - Filter by Goal A + March date range
        val startDate = Instant.parse("2024-03-01T00:00:00Z")
        val endDate = Instant.parse("2024-03-31T23:59:59Z")

        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules?startDate=$startDate&endDate=$endDate", goalA.id)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.title).isEqualTo("Goal A - March Schedule")
            assertThat(response.data?.first()?.goalId).isEqualTo(goalA.id)
          }
      }

    @Test
    fun `status와 날짜 범위 조합 필터링 성공`() =
      runTest {
        // given
        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "March Ready Schedule",
            description = "Content",
            startDate = Instant.parse("2024-03-15T00:00:00Z"),
            endDate = Instant.parse("2024-03-20T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "March Completed Schedule",
            description = "Content",
            startDate = Instant.parse("2024-03-21T00:00:00Z"),
            endDate = Instant.parse("2024-03-25T00:00:00Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "April Ready Schedule",
            description = "Content",
            startDate = Instant.parse("2024-04-10T00:00:00Z"),
            endDate = Instant.parse("2024-04-15T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "April Completed Schedule",
            description = "Content",
            startDate = Instant.parse("2024-04-20T00:00:00Z"),
            endDate = Instant.parse("2024-04-25T00:00:00Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        // when & then - Filter by READY status + March date range
        val startDate = Instant.parse("2024-03-01T00:00:00Z")
        val endDate = Instant.parse("2024-03-31T23:59:59Z")

        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules?status=READY&startDate=$startDate&endDate=$endDate", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.title).isEqualTo("March Ready Schedule")
            assertThat(response.data?.first()?.status).isEqualTo(ScheduleStatus.READY)
          }
      }

    @Test
    fun `모든 필터 조건 조합 테스트`() =
      runTest {
        // given
        val goalA =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Goal A",
              description = "Goal A Description",
              targetDate = Instant.parse("2024-04-15T00:00:00Z"),
              status = GoalStatus.READY,
            ),
          )

        val goalB =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Goal B",
              description = "Goal B Description",
              targetDate = Instant.parse("2024-05-15T00:00:00Z"),
              status = GoalStatus.READY,
            ),
          )

        // Target schedule (Goal A, READY, March)
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalA.id!!,
            userId = testUser.id,
            title = "Target Schedule",
            description = "Content",
            startDate = Instant.parse("2024-03-15T00:00:00Z"),
            endDate = Instant.parse("2024-03-20T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        // Other schedules that should not match
        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalA.id!!,
            userId = testUser.id,
            title = "Goal A - Completed March",
            description = "Content",
            startDate = Instant.parse("2024-03-21T00:00:00Z"),
            endDate = Instant.parse("2024-03-25T00:00:00Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalA.id!!,
            userId = testUser.id,
            title = "Goal A - Ready April",
            description = "Content",
            startDate = Instant.parse("2024-04-10T00:00:00Z"),
            endDate = Instant.parse("2024-04-15T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = goalB.id!!,
            userId = testUser.id,
            title = "Goal B - Ready March",
            description = "Content",
            startDate = Instant.parse("2024-03-10T00:00:00Z"),
            endDate = Instant.parse("2024-03-15T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        // when & then - Filter by all conditions: Goal A + READY + March
        val startDate = Instant.parse("2024-03-01T00:00:00Z")
        val endDate = Instant.parse("2024-03-31T23:59:59Z")

        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri(
            "/api/v1/goals/{goalId}/schedules?status=READY&startDate=$startDate&endDate=$endDate",
            goalA.id,
          ).exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.title).isEqualTo("Target Schedule")
            assertThat(response.data?.first()?.goalId).isEqualTo(goalA.id)
            assertThat(response.data?.first()?.status).isEqualTo(ScheduleStatus.READY)
          }
      }

    @Test
    fun `필터링 조건에 맞는 스케줄이 없을 때 빈 배열 반환`() =
      runTest {
        // given
        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "Existing Schedule",
            description = "Content",
            startDate = Instant.parse("2024-03-15T00:00:00Z"),
            endDate = Instant.parse("2024-03-20T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        // when & then - Filter by non-existent goalId
        val nonExistentGoalId = UUID.randomUUID()
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules", nonExistentGoalId)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).isEmpty()
          }

        // when & then - Filter by date range with no matching schedules
        val futureStartDate = Instant.parse("2025-01-01T00:00:00Z")
        val futureEndDate = Instant.parse("2025-01-31T23:59:59Z")
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules?startDate=$futureStartDate&endDate=$futureEndDate", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).isEmpty()
          }
      }

    @Test
    fun `날짜 범위 경계값 테스트`() =
      runTest {
        // given - Schedules on exact boundary dates
        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "Boundary Start Schedule",
            description = "Content",
            startDate = Instant.parse("2024-03-01T00:00:00Z"),
            endDate = Instant.parse("2024-03-05T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "Boundary End Schedule",
            description = "Content",
            startDate = Instant.parse("2024-03-30T00:00:00Z"),
            endDate = Instant.parse("2024-03-31T23:59:59Z"),
            status = ScheduleStatus.READY,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "Outside Boundary Schedule",
            description = "Content",
            startDate = Instant.parse("2024-04-01T00:00:00Z"),
            endDate = Instant.parse("2024-04-05T00:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        // when & then - Filter by exact March boundaries
        val startDate = Instant.parse("2024-03-01T00:00:00Z")
        val endDate = Instant.parse("2024-03-31T23:59:59Z")

        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules?startDate=$startDate&endDate=$endDate", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(2)
            val titles = response.data?.map { it.title }
            assertThat(titles).containsExactlyInAnyOrder("Boundary Start Schedule", "Boundary End Schedule")
          }
      }

    @Test
    fun `필터링과 페이징 조합 테스트`() =
      runTest {
        // given - Create 5 schedules that match the filter condition
        repeat(5) { index ->
          scheduleRepository.save(
            ScheduleEntity(
              goalId = testGoal.id!!,
              userId = testUser.id,
              title = "Schedule ${index + 1}",
              description = "Content ${index + 1}",
              startDate = Instant.parse("2024-03-${(index + 1).toString().padStart(2, '0')}T00:00:00Z"),
              endDate = Instant.parse("2024-03-${(index + 1).toString().padStart(2, '0')}T23:59:59Z"),
              status = ScheduleStatus.READY,
            ),
          )
        }

        // Create a schedule that doesn't match the filter (different status)
        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "Completed Schedule",
            description = "Content",
            startDate = Instant.parse("2024-03-10T00:00:00Z"),
            endDate = Instant.parse("2024-03-10T23:59:59Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        // when & then - First page with filter
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules?status=READY&page=0&size=2", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(2)
            response.data?.forEach { schedule ->
              assertThat(schedule.status).isEqualTo(ScheduleStatus.READY)
            }
          }

        // when & then - Second page with filter
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules?status=READY&page=1&size=2", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(2)
            response.data?.forEach { schedule ->
              assertThat(schedule.status).isEqualTo(ScheduleStatus.READY)
            }
          }

        // when & then - Third page with filter
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules?status=READY&page=2&size=2", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            response.data?.forEach { schedule ->
              assertThat(schedule.status).isEqualTo(ScheduleStatus.READY)
            }
          }
      }

    @Test
    fun `일정 목록 조회 시 시작 날짜 기준 오름차순 정렬 확인`() =
      runTest {
        // given - 시작 날짜가 다른 3개의 일정을 역순으로 생성
        val schedule3 =
          scheduleRepository.save(
            ScheduleEntity(
              goalId = testGoal.id!!,
              userId = testUser.id,
              title = "Third Schedule",
              description = "Latest start date",
              startDate = Instant.parse("2024-03-20T00:00:00Z"),
              endDate = Instant.parse("2024-03-25T00:00:00Z"),
              status = ScheduleStatus.READY,
            ),
          )

        val schedule1 =
          scheduleRepository.save(
            ScheduleEntity(
              goalId = testGoal.id!!,
              userId = testUser.id,
              title = "First Schedule",
              description = "Earliest start date",
              startDate = Instant.parse("2024-03-10T00:00:00Z"),
              endDate = Instant.parse("2024-03-15T00:00:00Z"),
              status = ScheduleStatus.READY,
            ),
          )

        val schedule2 =
          scheduleRepository.save(
            ScheduleEntity(
              goalId = testGoal.id!!,
              userId = testUser.id,
              title = "Second Schedule",
              description = "Middle start date",
              startDate = Instant.parse("2024-03-15T00:00:00Z"),
              endDate = Instant.parse("2024-03-20T00:00:00Z"),
              status = ScheduleStatus.READY,
            ),
          )

        // when & then - 일정 목록 조회 시 시작 날짜 기준 오름차순 정렬 확인
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/{goalId}/schedules", testGoal.id!!)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ScheduleResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(3)

            // 시작 날짜 기준 오름차순으로 정렬되어 있는지 확인
            val schedules = response.data!!
            assertThat(schedules[0].title).isEqualTo("First Schedule")
            assertThat(schedules[0].startDate).isEqualTo(Instant.parse("2024-03-10T00:00:00Z"))

            assertThat(schedules[1].title).isEqualTo("Second Schedule")
            assertThat(schedules[1].startDate).isEqualTo(Instant.parse("2024-03-15T00:00:00Z"))

            assertThat(schedules[2].title).isEqualTo("Third Schedule")
            assertThat(schedules[2].startDate).isEqualTo(Instant.parse("2024-03-20T00:00:00Z"))

            // 전체적으로 시작 날짜가 오름차순인지 확인
            for (i in 0 until schedules.size - 1) {
              assertThat(schedules[i].startDate).isBeforeOrEqualTo(schedules[i + 1].startDate)
            }
          }
      }

    @Test
    fun `월별 통계 조회 성공`() =
      runTest {
        // given - 2024년 3월에 다양한 상태의 일정들 생성
        // 3월 1일: 총 3개, 완료 2개 (완료율: 0.67)
        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "March 1 - Schedule 1",
            description = "Content",
            startDate = Instant.parse("2024-03-01T00:00:00Z"),
            endDate = Instant.parse("2024-03-01T23:59:59Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "March 1 - Schedule 2",
            description = "Content",
            startDate = Instant.parse("2024-03-01T10:00:00Z"),
            endDate = Instant.parse("2024-03-01T11:00:00Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "March 1 - Schedule 3",
            description = "Content",
            startDate = Instant.parse("2024-03-01T14:00:00Z"),
            endDate = Instant.parse("2024-03-01T15:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        // 3월 15일: 총 2개, 완료 1개 (완료율: 0.5)
        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "March 15 - Schedule 1",
            description = "Content",
            startDate = Instant.parse("2024-03-15T00:00:00Z"),
            endDate = Instant.parse("2024-03-15T23:59:59Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "March 15 - Schedule 2",
            description = "Content",
            startDate = Instant.parse("2024-03-15T10:00:00Z"),
            endDate = Instant.parse("2024-03-15T11:00:00Z"),
            status = ScheduleStatus.READY,
          ),
        )

        // 다른 월 일정 (결과에 포함되지 않아야 함)
        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "April Schedule",
            description = "Content",
            startDate = Instant.parse("2024-04-01T00:00:00Z"),
            endDate = Instant.parse("2024-04-01T23:59:59Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/schedules/monthly-statistics?year=2024&month=3")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<MonthlyScheduleStatisticsResponse>>()
          .value { response ->
            assertThat(response.data?.year).isEqualTo(2024)
            assertThat(response.data?.month).isEqualTo(3)
            assertThat(response.data?.dailyStatistics).hasSize(2)

            val march1Stats = response.data?.dailyStatistics?.find { it.date == 1 }
            assertThat(march1Stats?.totalCount).isEqualTo(3)
            assertThat(march1Stats?.completedCount).isEqualTo(2)
            assertThat(march1Stats?.completionRate).isEqualTo(2.0 / 3.0)

            val march15Stats = response.data?.dailyStatistics?.find { it.date == 15 }
            assertThat(march15Stats?.totalCount).isEqualTo(2)
            assertThat(march15Stats?.completedCount).isEqualTo(1)
            assertThat(march15Stats?.completionRate).isEqualTo(0.5)
          }
      }

    @Test
    fun `일정이 없는 월 통계 조회 - 빈 결과 반환`() =
      runTest {
        // given - 3월이 아닌 다른 월에만 일정 생성
        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "April Schedule",
            description = "Content",
            startDate = Instant.parse("2024-04-01T00:00:00Z"),
            endDate = Instant.parse("2024-04-01T23:59:59Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        // when & then - 3월 통계 조회
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/schedules/monthly-statistics?year=2024&month=3")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<MonthlyScheduleStatisticsResponse>>()
          .value { response ->
            assertThat(response.data?.year).isEqualTo(2024)
            assertThat(response.data?.month).isEqualTo(3)
            assertThat(response.data?.dailyStatistics).isEmpty()
          }
      }

    @Test
    fun `완료율 계산 정확성 테스트`() =
      runTest {
        // given - 모든 일정이 완료된 날과 미완료된 날 생성
        // 3월 1일: 총 2개, 완료 2개 (완료율: 1.0)
        repeat(2) {
          scheduleRepository.save(
            ScheduleEntity(
              goalId = testGoal.id!!,
              userId = testUser.id,
              title = "March 1 - Completed ${it + 1}",
              description = "Content",
              startDate = Instant.parse("2024-03-01T0$it:00:00Z"),
              endDate = Instant.parse("2024-03-01T0$it:59:59Z"),
              status = ScheduleStatus.COMPLETED,
            ),
          )
        }

        // 3월 2일: 총 3개, 완료 0개 (완료율: 0.0)
        repeat(3) {
          scheduleRepository.save(
            ScheduleEntity(
              goalId = testGoal.id!!,
              userId = testUser.id,
              title = "March 2 - Ready ${it + 1}",
              description = "Content",
              startDate = Instant.parse("2024-03-02T0$it:00:00Z"),
              endDate = Instant.parse("2024-03-02T0$it:59:59Z"),
              status = ScheduleStatus.READY,
            ),
          )
        }

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/schedules/monthly-statistics?year=2024&month=3")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<MonthlyScheduleStatisticsResponse>>()
          .value { response ->
            assertThat(response.data?.dailyStatistics).hasSize(2)

            val march1Stats = response.data?.dailyStatistics?.find { it.date == 1 }
            assertThat(march1Stats?.completionRate).isEqualTo(1.0)

            val march2Stats = response.data?.dailyStatistics?.find { it.date == 2 }
            assertThat(march2Stats?.completionRate).isEqualTo(0.0)
          }
      }

    @Test
    fun `인증되지 않은 사용자 401 반환`() =
      runTest {
        // when & then
        webTestClient
          .get()
          .uri("/api/v1/goals/schedules/monthly-statistics?year=2024&month=3")
          .exchange()
          .expectStatus()
          .isUnauthorized
      }

    @Test
    fun `잘못된 월 파라미터로 요청시 정상 처리`() =
      runTest {
        // when & then - 13월로 요청
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/schedules/monthly-statistics?year=2024&month=13")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<MonthlyScheduleStatisticsResponse>>()
          .value { response ->
            assertThat(response.data?.year).isEqualTo(2024)
            assertThat(response.data?.month).isEqualTo(13)
            assertThat(response.data?.dailyStatistics).isEmpty()
          }
      }

    @Test
    fun `다른 사용자의 일정은 통계에 포함되지 않음`() =
      runTest {
        // given - 다른 사용자 생성
        val otherUser =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.GOOGLE,
              providerId = "other-user-123",
            ),
          )

        val otherUserGoal =
          goalRepository.save(
            GoalEntity(
              userId = otherUser.id!!,
              title = "Other User Goal",
              description = "Description",
              targetDate = Instant.parse("2024-04-15T00:00:00Z"),
              status = GoalStatus.READY,
            ),
          )

        // 현재 테스트 사용자의 일정
        scheduleRepository.save(
          ScheduleEntity(
            goalId = testGoal.id!!,
            userId = testUser.id,
            title = "Test User Schedule",
            description = "Content",
            startDate = Instant.parse("2024-03-01T00:00:00Z"),
            endDate = Instant.parse("2024-03-01T23:59:59Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        // 다른 사용자의 일정 (결과에 포함되지 않아야 함)
        scheduleRepository.save(
          ScheduleEntity(
            goalId = otherUserGoal.id!!,
            userId = otherUser.id!!,
            title = "Other User Schedule",
            description = "Content",
            startDate = Instant.parse("2024-03-01T00:00:00Z"),
            endDate = Instant.parse("2024-03-01T23:59:59Z"),
            status = ScheduleStatus.COMPLETED,
          ),
        )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/goals/schedules/monthly-statistics?year=2024&month=3")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<MonthlyScheduleStatisticsResponse>>()
          .value { response ->
            assertThat(response.data?.dailyStatistics).hasSize(1)
            val march1Stats = response.data?.dailyStatistics?.first()
            assertThat(march1Stats?.totalCount).isEqualTo(1)
            assertThat(march1Stats?.completedCount).isEqualTo(1)
          }
      }
  }
