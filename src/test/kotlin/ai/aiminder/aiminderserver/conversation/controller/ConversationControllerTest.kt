package ai.aiminder.aiminderserver.conversation.controller

import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.conversation.dto.ConversationResponse
import ai.aiminder.aiminderserver.conversation.entity.ConversationEntity
import ai.aiminder.aiminderserver.conversation.repository.ConversationRepository
import ai.aiminder.aiminderserver.goal.domain.GoalStatus
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication
import org.springframework.test.web.reactive.server.expectBody
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class ConversationControllerTest
  @Autowired
  constructor(
    private val userRepository: UserRepository,
    private val goalRepository: GoalRepository,
    private val conversationRepository: ConversationRepository,
    private val jdbcTemplate: JdbcTemplate,
  ) : BaseIntegrationTest() {
    private lateinit var testUser: User
    private lateinit var otherUser: User
    private lateinit var testGoal: GoalEntity
    private lateinit var authentication: UsernamePasswordAuthenticationToken
    private lateinit var otherUserAuthentication: UsernamePasswordAuthenticationToken

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

        // 다른 사용자 생성 (권한 테스트용)
        val savedOtherUser =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.KAKAO,
              providerId = "other-provider-456",
            ),
          )
        otherUser = User.from(savedOtherUser)
        otherUserAuthentication =
          UsernamePasswordAuthenticationToken(
            otherUser,
            null,
            listOf(SimpleGrantedAuthority(Role.USER.name)),
          )
      }

    @Test
    fun `대화 목록 조회 성공`() =
      runTest {
        // given
        val conversation1 =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
              goalId = testGoal.id,
            ),
          )

        val conversation2 =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
              goalId = null,
            ),
          )

        // Add chat memory data for recent chat and timestamp
        jdbcTemplate.update(
          """
          INSERT INTO spring_ai_chat_memory (conversation_id, content, type, timestamp) 
          VALUES (?, ?, ?, ?)
          """.trimIndent(),
          conversation1.id.toString(),
          "Hello, this is the first conversation",
          "USER",
          LocalDateTime.now().minusHours(1),
        )

        jdbcTemplate.update(
          """
          INSERT INTO spring_ai_chat_memory (conversation_id, content, type, timestamp) 
          VALUES (?, ?, ?, ?)
          """.trimIndent(),
          conversation2.id.toString(),
          "Hello, this is the second conversation",
          "USER",
          LocalDateTime.now().minusHours(2),
        )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/conversations")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ConversationResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(2)
            assertThat(response.data?.first()?.conversationId).isEqualTo(conversation1.id)
            assertThat(response.data?.first()?.recentChat).isEqualTo("Hello, this is the first conversation")
            assertThat(response.data?.first()?.goalId).isEqualTo(testGoal.id)
            assertThat(response.data?.first()?.goalTitle).isEqualTo("Test Goal")

            assertThat(response.data?.get(1)?.conversationId).isEqualTo(conversation2.id)
            assertThat(response.data?.get(1)?.recentChat).isEqualTo("Hello, this is the second conversation")
            assertThat(response.data?.get(1)?.goalId).isNull()
            assertThat(response.data?.get(1)?.goalTitle).isNull()
          }
      }

    @Test
    fun `인증되지 않은 사용자는 401 반환`() =
      runTest {
        // when & then
        webTestClient
          .get()
          .uri("/api/v1/conversations")
          .exchange()
          .expectStatus()
          .isUnauthorized
      }

    @Test
    fun `대화가 없는 사용자의 경우 빈 배열 반환`() =
      runTest {
        // when & then (no conversations created)
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/conversations")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ConversationResponse>>>()
          .value { response ->
            assertThat(response.data).isEmpty()
          }
      }

    @Test
    fun `삭제된 대화는 목록에 포함되지 않음`() =
      runTest {
        // given
        val activeConversation =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
              goalId = testGoal.id,
            ),
          )

        val deletedConversation =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
              goalId = testGoal.id,
              deletedAt = Instant.now(),
            ),
          )

        // Add chat memory for active conversation
        jdbcTemplate.update(
          """
          INSERT INTO spring_ai_chat_memory (conversation_id, content, type, timestamp) 
          VALUES (?, ?, ?, ?)
          """.trimIndent(),
          activeConversation.id.toString(),
          "Active conversation message",
          "USER",
          LocalDateTime.now().minusHours(1),
        )

        // Add chat memory for deleted conversation
        jdbcTemplate.update(
          """
          INSERT INTO spring_ai_chat_memory (conversation_id, content, type, timestamp) 
          VALUES (?, ?, ?, ?)
          """.trimIndent(),
          deletedConversation.id.toString(),
          "Deleted conversation message",
          "USER",
          LocalDateTime.now().minusHours(2),
        )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/conversations")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ConversationResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.conversationId).isEqualTo(activeConversation.id)
          }
      }

    @Test
    fun `대화 목록이 recentAt 기준으로 내림차순 정렬됨`() =
      runTest {
        // given
        val conversation1 =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
            ),
          )

        val conversation2 =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
            ),
          )

        val conversation3 =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
            ),
          )

        val now = LocalDateTime.now()

        // Add chat memory with different timestamps
        jdbcTemplate.update(
          """
          INSERT INTO spring_ai_chat_memory (conversation_id, content, type, timestamp) 
          VALUES (?, ?, ?, ?)
          """.trimIndent(),
          conversation1.id.toString(),
          "Oldest conversation",
          "USER",
          now.minusHours(3),
        )

        jdbcTemplate.update(
          """
          INSERT INTO spring_ai_chat_memory (conversation_id, content, type, timestamp) 
          VALUES (?, ?, ?, ?)
          """.trimIndent(),
          conversation2.id.toString(),
          "Most recent conversation",
          "USER",
          now.minusHours(1),
        )

        jdbcTemplate.update(
          """
          INSERT INTO spring_ai_chat_memory (conversation_id, content, type, timestamp) 
          VALUES (?, ?, ?, ?)
          """.trimIndent(),
          conversation3.id.toString(),
          "Middle conversation",
          "USER",
          now.minusHours(2),
        )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/conversations")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ConversationResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(3)
            assertThat(response.data?.get(0)?.recentChat).isEqualTo("Most recent conversation")
            assertThat(response.data?.get(1)?.recentChat).isEqualTo("Middle conversation")
            assertThat(response.data?.get(2)?.recentChat).isEqualTo("Oldest conversation")
          }
      }

    @Test
    fun `페이징 동작 확인`() =
      runTest {
        // given - Create 5 conversations
        val conversations = mutableListOf<ConversationEntity>()
        repeat(5) { index ->
          val conversation =
            conversationRepository.save(
              ConversationEntity(
                userId = testUser.id,
              ),
            )
          conversations.add(conversation)

          jdbcTemplate.update(
            """
            INSERT INTO spring_ai_chat_memory (conversation_id, content, type, timestamp) 
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
            conversation.id.toString(),
            "Conversation ${index + 1}",
            "USER",
            LocalDateTime.now().minusHours(index.toLong()),
          )
        }

        // when & then - First page (size = 2)
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/conversations?page=0&size=2")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ConversationResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(2)
            assertThat(response.data?.get(0)?.recentChat).isEqualTo("Conversation 1")
            assertThat(response.data?.get(1)?.recentChat).isEqualTo("Conversation 2")
          }

        // when & then - Second page
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/conversations?page=1&size=2")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ConversationResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(2)
            assertThat(response.data?.get(0)?.recentChat).isEqualTo("Conversation 3")
            assertThat(response.data?.get(1)?.recentChat).isEqualTo("Conversation 4")
          }

        // when & then - Third page (last item)
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/conversations?page=2&size=2")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ConversationResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.get(0)?.recentChat).isEqualTo("Conversation 5")
          }
      }

    @Test
    fun `존재하지 않는 페이지 요청시 빈 배열 반환`() =
      runTest {
        // given - Only 1 conversation
        val conversation =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
            ),
          )

        jdbcTemplate.update(
          """
          INSERT INTO spring_ai_chat_memory (conversation_id, content, type, timestamp) 
          VALUES (?, ?, ?, ?)
          """.trimIndent(),
          conversation.id.toString(),
          "Only conversation",
          "USER",
          LocalDateTime.now(),
        )

        // when & then - Request page 1 (second page) when only 1 item exists
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/conversations?page=1&size=10")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ConversationResponse>>>()
          .value { response ->
            assertThat(response.data).isEmpty()
          }
      }

    @Test
    fun `다른 사용자의 대화는 조회되지 않음`() =
      runTest {
        // given - Create another user
        val anotherUserEntity =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.KAKAO,
              providerId = "another-provider-456",
            ),
          )
        val anotherUser = User.from(anotherUserEntity)

        // Create conversations for both users
        val testUserConversation =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
            ),
          )

        val anotherUserConversation =
          conversationRepository.save(
            ConversationEntity(
              userId = anotherUser.id,
            ),
          )

        // Add chat memory for both conversations
        jdbcTemplate.update(
          """
          INSERT INTO spring_ai_chat_memory (conversation_id, content, type, timestamp) 
          VALUES (?, ?, ?, ?)
          """.trimIndent(),
          testUserConversation.id.toString(),
          "Test user conversation",
          "USER",
          LocalDateTime.now().minusHours(1),
        )

        jdbcTemplate.update(
          """
          INSERT INTO spring_ai_chat_memory (conversation_id, content, type, timestamp) 
          VALUES (?, ?, ?, ?)
          """.trimIndent(),
          anotherUserConversation.id.toString(),
          "Another user conversation",
          "USER",
          LocalDateTime.now().minusHours(2),
        )

        // when & then - Only testUser's conversation should be returned
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/conversations")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ConversationResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.conversationId).isEqualTo(testUserConversation.id)
            assertThat(response.data?.first()?.recentChat).isEqualTo("Test user conversation")
          }
      }

    @Test
    fun `Goal이 삭제된 경우 goalTitle은 null로 반환됨`() =
      runTest {
        // given - Create a goal and then delete it
        val deletedGoal =
          goalRepository.save(
            GoalEntity(
              userId = testUser.id,
              title = "Deleted Goal",
              description = "This goal will be deleted",
              targetDate = Instant.parse("2024-05-15T00:00:00Z"),
              status = GoalStatus.READY,
              deletedAt = Instant.now(),
            ),
          )

        val conversation =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
              goalId = deletedGoal.id,
            ),
          )

        jdbcTemplate.update(
          """
          INSERT INTO spring_ai_chat_memory (conversation_id, content, type, timestamp) 
          VALUES (?, ?, ?, ?)
          """.trimIndent(),
          conversation.id.toString(),
          "Conversation with deleted goal",
          "USER",
          LocalDateTime.now(),
        )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/conversations")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ConversationResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.goalId).isEqualTo(deletedGoal.id)
            assertThat(response.data?.first()?.goalTitle).isNull()
          }
      }

    @Test
    fun `대화에 채팅 기록이 없는 경우 빈 문자열 반환`() =
      runTest {
        // given - Conversation without chat memory
        val conversation =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
            ),
          )

        // when & then
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/conversations")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ConversationResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.conversationId).isEqualTo(conversation.id)
            assertThat(response.data?.first()?.recentChat).isEmpty()
          }
      }

    @Test
    fun `최소 페이징 파라미터 동작 확인`() =
      runTest {
        // given
        val conversation =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
            ),
          )

        jdbcTemplate.update(
          """
          INSERT INTO spring_ai_chat_memory (conversation_id, content, type, timestamp)
          VALUES (?, ?, ?, ?)
          """.trimIndent(),
          conversation.id.toString(),
          "Single conversation",
          "USER",
          LocalDateTime.now(),
        )

        // when & then - Minimum paging (page=0, size=1)
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/conversations?page=0&size=1")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<ServiceResponse<List<ConversationResponse>>>()
          .value { response ->
            assertThat(response.data).hasSize(1)
            assertThat(response.data?.first()?.conversationId).isEqualTo(conversation.id)
          }
      }

    @Test
    fun `대화 삭제 성공`() =
      runTest {
        // given
        val conversation =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
              goalId = testGoal.id,
            ),
          )

        // when
        val response = deleteConversation(conversation.id!!)

        // then
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isEqualTo("Conversation deleted successfully")

        // 실제로 데이터베이스에서 삭제되었는지 확인 (soft delete)
        val deletedConversation = conversationRepository.findById(conversation.id!!)
        assertThat(deletedConversation).isNotNull()
        assertThat(deletedConversation!!.deletedAt).isNotNull()
      }

    @Test
    fun `인증되지 않은 사용자는 대화 삭제 시 401 반환`() =
      runTest {
        // given
        val conversation =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
            ),
          )

        // when & then
        webTestClient
          .delete()
          .uri("/api/v1/conversations/${conversation.id}")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isUnauthorized
      }

    @Test
    fun `존재하지 않는 대화 삭제 시 404 반환`() {
      // given
      val nonExistentConversationId = UUID.randomUUID()

      // when
      val response = deleteConversationExpectingError(nonExistentConversationId)

      // then
      assertThat(response.statusCode).isEqualTo(404)
      assertThat(response.errorCode).isEqualTo("ASSISTANT:CONVERSATIONNOTFOUND")
    }

    @Test
    fun `다른 사용자의 대화 삭제 시 401 반환`() =
      runTest {
        // given
        val conversation =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
            ),
          )

        // when
        val response = deleteConversationExpectingError(conversation.id!!, otherUserAuthentication)

        // then
        assertThat(response.statusCode).isEqualTo(401)
        assertThat(response.errorCode).isEqualTo("AUTH:UNAUTHORIZED")
      }

    @Test
    fun `이미 삭제된 대화 삭제 시 404 반환`() =
      runTest {
        // given - 이미 삭제된 대화 생성
        val deletedConversation =
          conversationRepository.save(
            ConversationEntity(
              userId = testUser.id,
              deletedAt = Instant.now(),
            ),
          )

        // when
        val response = deleteConversationExpectingError(deletedConversation.id!!)

        // then
        assertThat(response.statusCode).isEqualTo(404)
        assertThat(response.errorCode).isEqualTo("ASSISTANT:CONVERSATIONNOTFOUND")
      }

    @Test
    fun `잘못된 UUID 형식으로 대화 삭제 요청 시 400 반환`() {
      // when & then
      webTestClient
        .mutateWith(mockAuthentication(authentication))
        .delete()
        .uri("/api/v1/conversations/invalid-uuid-format")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    private fun deleteConversation(
      conversationId: UUID,
      auth: UsernamePasswordAuthenticationToken = authentication,
    ): ServiceResponse<String> =
      webTestClient
        .mutateWith(mockAuthentication(auth))
        .delete()
        .uri("/api/v1/conversations/$conversationId")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ServiceResponse<String>>()
        .returnResult()
        .responseBody!!

    private fun deleteConversationExpectingError(
      conversationId: UUID,
      auth: UsernamePasswordAuthenticationToken = authentication,
    ): ServiceResponse<Unit> =
      webTestClient
        .mutateWith(mockAuthentication(auth))
        .delete()
        .uri("/api/v1/conversations/$conversationId")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody<ServiceResponse<Unit>>()
        .returnResult()
        .responseBody!!
  }
