package ai.aiminder.aiminderserver.goal.controller

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.User
import ai.aiminder.aiminderserver.auth.entity.UserEntity
import ai.aiminder.aiminderserver.auth.repository.UserRepository
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class GoalControllerTest
  @Autowired
  constructor(
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
              providerId = "test-provider-123",
            ),
          )
        testUser = User.from(savedUser)
      }

    @Test
    fun `정상적인 Goal 생성 테스트`() {
      // given
      val request =
        CreateGoalRequest(
          title = "Learn Kotlin",
          description = "Master Kotlin programming language by reading documentation and building projects",
          // 30 days from now
          targetDate = Instant.now().plusSeconds(86400 * 30),
        )

      // when & then
      val authentication =
        UsernamePasswordAuthenticationToken(
          testUser,
          null,
          listOf(SimpleGrantedAuthority("ROLE_USER")),
        )

      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .post()
        .uri("/api/goal")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk // GoalController doesn't return 201 Created, it returns 200 OK
        .expectBody()
        .jsonPath("$.statusCode")
        .isEqualTo(200)
        .jsonPath("$.data")
        .exists()
        .jsonPath("$.data.title")
        .isEqualTo("Learn Kotlin")
        .jsonPath(
          "$.data.description",
        ).isEqualTo("Master Kotlin programming language by reading documentation and building projects")
        .jsonPath("$.data.status")
        .isEqualTo("ACTIVE")
        .jsonPath("$.data.isAiGenerated")
        .isEqualTo(false)
        .jsonPath("$.data.userId")
        .isEqualTo(testUser.id.toString())

      // Verify goal was actually saved to database
      val goalExistsQuery = "SELECT COUNT(*) FROM goals WHERE user_id = $1 AND title = $2"
      StepVerifier
        .create(
          databaseClient
            .sql(goalExistsQuery)
            .bind(0, testUser.id)
            .bind(1, "Learn Kotlin")
            .map { row -> row.get(0, Long::class.java) }
            .one(),
        ).expectNext(1L)
        .verifyComplete()
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

      // when & then
      webTestClient
        .post()
        .uri("/api/goal")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `빈 제목으로 요청 시 400 Bad Request 반환`() {
      // given
      val requestWithEmptyTitle =
        mapOf(
          "title" to "",
          "description" to "Valid description",
          "targetDate" to Instant.now().plusSeconds(86400).toString(),
        )

      // when & then
      val authentication =
        UsernamePasswordAuthenticationToken(
          testUser,
          null,
          listOf(SimpleGrantedAuthority("ROLE_USER")),
        )

      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .post()
        .uri("/api/goal")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestWithEmptyTitle)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `필수 필드 누락 시 400 Bad Request 반환`() {
      // given - title 필드 누락
      val requestMissingTitle =
        mapOf(
          "description" to "Valid description",
          "targetDate" to Instant.now().plusSeconds(86400).toString(),
        )

      // when & then
      val authentication =
        UsernamePasswordAuthenticationToken(
          testUser,
          null,
          listOf(SimpleGrantedAuthority("ROLE_USER")),
        )

      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .post()
        .uri("/api/goal")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestMissingTitle)
        .exchange()
        .expectStatus()
        .isBadRequest
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
      val authentication =
        UsernamePasswordAuthenticationToken(
          testUser,
          null,
          listOf(SimpleGrantedAuthority("ROLE_USER")),
        )

      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .post()
        .uri("/api/goal")
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

      // when & then
      val authentication =
        UsernamePasswordAuthenticationToken(
          nonExistentUser,
          null,
          listOf(SimpleGrantedAuthority("ROLE_USER")),
        )

      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .post()
        .uri("/api/goal")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .is5xxServerError // This might be 500 or another error depending on implementation
    }
  }
