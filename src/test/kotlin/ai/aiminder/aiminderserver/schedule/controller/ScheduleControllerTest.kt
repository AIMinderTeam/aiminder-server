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
            goalId = testGoal.id!!,
            title = "Test Schedule",
            description = "Test Schedule Content",
            startDate = Instant.parse("2024-03-15T00:00:00Z"),
            endDate = Instant.parse("2024-03-22T00:00:00Z"),
          )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/schedules")
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
          .uri("/api/v1/schedules")
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
          .uri("/api/v1/schedules/${schedule.id}")
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
          .uri("/api/v1/schedules/${schedule.id}")
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
          .uri("/api/v1/schedules/${schedule.id}")
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
          .uri("/api/v1/schedules/$nonExistentId")
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
          .uri("/api/v1/schedules")
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
  }
