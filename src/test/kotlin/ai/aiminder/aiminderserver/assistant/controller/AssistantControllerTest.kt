package ai.aiminder.aiminderserver.assistant.controller

import ai.aiminder.aiminderserver.assistant.client.GoalAssistantClient
import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.domain.AssistantResponseType
import ai.aiminder.aiminderserver.assistant.domain.ChatResponseDto
import ai.aiminder.aiminderserver.assistant.domain.ChatType
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequest
import ai.aiminder.aiminderserver.assistant.dto.ChatResponse
import ai.aiminder.aiminderserver.assistant.entity.ChatEntity
import ai.aiminder.aiminderserver.assistant.repository.ChatRepository
import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.conversation.entity.ConversationEntity
import ai.aiminder.aiminderserver.conversation.repository.ConversationRepository
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.entity.UserEntity
import ai.aiminder.aiminderserver.user.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearMocks
import io.mockk.coEvery
import kotlinx.coroutines.flow.toList
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

class AssistantControllerTest
  @Autowired
  constructor(
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository,
    private val chatRepository: ChatRepository,
    private val objectMapper: ObjectMapper,
  ) : BaseIntegrationTest() {
    @MockkBean
    private lateinit var assistantClient: GoalAssistantClient
    private lateinit var testUser: User
    private lateinit var authentication: UsernamePasswordAuthenticationToken

    @BeforeEach
    fun setUp() =
      runTest {
        // Clear all mocks before each test
        clearMocks(assistantClient)

        val savedUser =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.GOOGLE,
              providerId = "test-assistant-user-123",
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
    fun `정상적인 채팅 시작 테스트`() =
      runTest {
        // given & when
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/chat")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<ChatResponse>>()
            .returnResult()
            .responseBody!!

        // then
        response.also {
          assertThat(it.statusCode).isEqualTo(200)
          assertThat(it.data).isNotNull
          assertThat(it.data?.chat).isNotEmpty
          assertThat(
            it.data
              ?.chat
              ?.first()
              ?.messages,
          ).isNotEmpty
        }

        // 새로운 Conversation이 생성되었는지 확인
        val conversations = conversationRepository.findAll().toList()
        assertThat(conversations).hasSize(1)
        assertThat(conversations.first().userId).isEqualTo(testUser.id)
      }

    @Test
    fun `인증되지 않은 사용자 채팅 시작 시 401 반환`() {
      // when
      val response =
        webTestClient
          .post()
          .uri("/api/v1/conversations/chat")
          .accept(MediaType.APPLICATION_JSON)
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
    fun `존재하지 않는 사용자로 채팅 시작 시 적절한 에러 처리`() {
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
          .post()
          .uri("/api/v1/conversations/chat")
          .accept(MediaType.APPLICATION_JSON)
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
    fun `정상적인 메시지 전송 테스트`() =
      runTest {
        // given - 대화방 생성
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )
        val request = AssistantRequest(text = "안녕하세요! 오늘 날씨가 어떤가요?")

        mockAssistantChatResponse(conversation, request)

        // when - AI 응답이 모킹되어 정상 처리 예상
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/${conversation.id}/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<ChatResponse>>()
            .returnResult()
            .responseBody!!

        // then - 정상 AI 응답 확인
        response.also {
          assertThat(it.statusCode).isEqualTo(200)
          assertThat(it.data).isNotNull
          assertThat(it.data?.chat).isNotEmpty
          assertThat(
            it.data
              ?.chat
              ?.first()
              ?.type,
          ).isEqualTo(AssistantResponseType.TEXT)
          assertThat(
            it.data
              ?.chat
              ?.first()
              ?.messages,
          ).contains("안녕하세요! 오늘은 맑고 화창한 날씨입니다. 기온은 22도 정도로 외출하기에 좋은 날씨네요!")
        }
      }

    private fun mockAssistantChatResponse(
      conversation: ConversationEntity,
      request: AssistantRequest,
    ) {
      val mockAIResponse =
        AssistantResponse(
          responses =
            listOf(
              ChatResponseDto(
                type = AssistantResponseType.TEXT,
                messages = listOf("안녕하세요! 오늘은 맑고 화창한 날씨입니다. 기온은 22도 정도로 외출하기에 좋은 날씨네요!"),
              ),
            ),
        )

      coEvery {
        assistantClient.chat(match { it.conversationId == conversation.id && it.text == request.text })
      } returns mockAIResponse
    }

    @Test
    fun `존재하지 않는 대화방으로 메시지 전송 시 404 NotFound 반환`() {
      // given
      val nonExistentConversationId = UUID.randomUUID()
      val request = AssistantRequest(text = "Hello!")

      // when
      val response =
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/conversations/$nonExistentConversationId/chat")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
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
    fun `잘못된 요청 형식으로 메시지 전송 시 400 Bad Request 반환`() =
      runTest {
        // given
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )
        val requestMissingText = mapOf("invalidField" to "value")

        // when
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/${conversation.id}/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestMissingText)
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
    fun `빈 메시지 전송 시 400 Bad Request 반환`() =
      runTest {
        // given
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )
        val emptyTextRequest = AssistantRequest(text = "")

        // when
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/${conversation.id}/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(emptyTextRequest)
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
          assertThat(it.message).isEqualTo("메시지 내용이 비어있습니다.")
        }
      }

    @Test
    fun `공백만 포함된 메시지 전송 시 400 Bad Request 반환`() =
      runTest {
        // given
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )
        val whitespaceOnlyRequest = AssistantRequest(text = "   ")

        // when
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/${conversation.id}/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(whitespaceOnlyRequest)
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
          assertThat(it.message).isEqualTo("메시지 내용이 비어있습니다.")
        }
      }

    @Test
    fun `인증되지 않은 사용자 메시지 전송 시 401 반환`() =
      runTest {
        // given
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )
        val request = AssistantRequest(text = "Hello!")

        // when
        val response =
          webTestClient
            .post()
            .uri("/api/v1/conversations/${conversation.id}/chat")
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
    fun `다른 사용자의 대화방 접근 시 401 Unauthorized 반환`() =
      runTest {
        // given - 다른 사용자 생성
        val anotherUser =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.KAKAO,
              providerId = "another-user-456",
            ),
          )

        val anotherUserDomain = User.from(anotherUser)
        val anotherUserConversation =
          conversationRepository.save(
            ConversationEntity.from(anotherUserDomain),
          )

        val request = AssistantRequest(text = "Hello!")

        // when - 다른 사용자의 대화방에 접근하므로 401 에러 발생
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/${anotherUserConversation.id}/chat")
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
        assertThat(response.errorCode).isEqualTo("AUTH:UNAUTHORIZED")
        assertThat(response.message).isEqualTo("인증이 필요합니다. 로그인을 진행해주세요.")
        assertThat(response.data).isNull()
      }

    @Test
    fun `전체 플로우 테스트 - 채팅 시작부터 메시지 전송까지`() =
      runTest {
        // given & when - 채팅 시작
        val startChatResponse =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/chat")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<ChatResponse>>()
            .returnResult()
            .responseBody!!

        // then - 채팅 시작 성공 확인
        assertThat(startChatResponse.data).isNotNull
        assertThat(startChatResponse.data?.chat).isNotEmpty

        // given - 생성된 대화방 확인
        val conversations = conversationRepository.findAll().toList()
        assertThat(conversations).hasSize(1)
        val conversationId = conversations.first().id!!

        // when - 메시지 전송
        val request = AssistantRequest(text = "안녕하세요!")

        // mock 설정
        val conversationEntity = conversationRepository.findById(conversationId)!!
        mockAssistantChatResponse(conversationEntity, request)

        val sendMessageResponse =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/$conversationId/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<ChatResponse>>()
            .returnResult()
            .responseBody!!

        // then - 정상 응답 확인
        assertThat(sendMessageResponse.statusCode).isEqualTo(200)
        assertThat(sendMessageResponse.data).isNotNull
      }

    @Test
    fun `연속된 메시지 교환 테스트`() =
      runTest {
        // given - 대화방 생성
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )

        val messages =
          listOf(
            "안녕하세요!",
            "오늘 날씨가 어떤가요?",
            "내일 할 일을 추천해주세요.",
          )

        // when & then - 연속된 메시지 전송
        messages.forEach { messageText ->
          val request = AssistantRequest(text = messageText)

          mockAssistantChatResponse(conversation, request)

          val response =
            webTestClient
              .mutateWith(mockAuthentication(authentication))
              .post()
              .uri("/api/v1/conversations/${conversation.id}/chat")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(request)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody<ServiceResponse<ChatResponse>>()
              .returnResult()
              .responseBody!!

          assertThat(response.statusCode).isEqualTo(200)
          assertThat(response.data).isNotNull
        }
      }

    // getMessages API 테스트들
    @Test
    fun `정상적인 메시지 조회 테스트 - 빈 대화방의 경우 빈 배열 반환`() =
      runTest {
        // given - 대화방 생성 (실제로는 Spring AI Chat Memory에 메시지가 저장되어야 하지만 테스트 환경에서는 빈 상태)
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )

        // when - 메시지 조회
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .get()
            .uri("/api/v1/conversations/${conversation.id}/chat")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<List<ChatResponse>>>()
            .returnResult()
            .responseBody!!

        // then - Spring AI Chat Memory가 빈 상태이므로 빈 배열 반환이 정상
        response.also {
          assertThat(it.statusCode).isEqualTo(200)
          assertThat(it.data).isNotNull
          assertThat(it.data).isEmpty() // 테스트 환경에서는 실제 AI 메모리가 없으므로 빈 배열
        }
      }

    @Test
    fun `페이징 파라미터를 사용한 메시지 조회 테스트`() =
      runTest {
        // given - 대화방 생성
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )

        // when - 페이지 크기 2로 첫 번째 페이지 조회
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .get()
            .uri("/api/v1/conversations/${conversation.id}/chat?page=0&size=2")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<List<ChatResponse>>>()
            .returnResult()
            .responseBody!!

        // then - 페이징 파라미터가 정상적으로 처리되고 빈 결과 반환
        response.also {
          assertThat(it.statusCode).isEqualTo(200)
          assertThat(it.data).isNotNull
          assertThat(it.data).isEmpty() // 테스트 환경에서는 메시지가 없으므로 빈 배열
        }
      }

    @Test
    fun `빈 대화방 메시지 조회 테스트`() =
      runTest {
        // given - 메시지가 없는 대화방 생성
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )

        // when - 메시지 조회
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .get()
            .uri("/api/v1/conversations/${conversation.id}/chat")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<List<ChatResponse>>>()
            .returnResult()
            .responseBody!!

        // then - 빈 배열 반환 확인
        response.also {
          assertThat(it.statusCode).isEqualTo(200)
          assertThat(it.data).isNotNull
          assertThat(it.data).isEmpty()
        }
      }

    @Test
    fun `인증되지 않은 사용자 메시지 조회 시 401 반환`() =
      runTest {
        // given
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )

        // when
        val response =
          webTestClient
            .get()
            .uri("/api/v1/conversations/${conversation.id}/chat")
            .accept(MediaType.APPLICATION_JSON)
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
    fun `존재하지 않는 대화방 메시지 조회 시 404 반환`() {
      // given
      val nonExistentConversationId = UUID.randomUUID()

      // when
      val response =
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/conversations/$nonExistentConversationId/chat")
          .accept(MediaType.APPLICATION_JSON)
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
    fun `다른 사용자의 대화방 메시지 조회 시 401 반환`() =
      runTest {
        // given - 다른 사용자 생성
        val anotherUser =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.KAKAO,
              providerId = "another-user-456",
            ),
          )

        val anotherUserDomain = User.from(anotherUser)
        val anotherUserConversation =
          conversationRepository.save(
            ConversationEntity.from(anotherUserDomain),
          )

        // when - 다른 사용자의 대화방 메시지 조회 시도
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .get()
            .uri("/api/v1/conversations/${anotherUserConversation.id}/chat")
            .accept(MediaType.APPLICATION_JSON)
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
    fun `잘못된 UUID 형식으로 메시지 조회 시 400 반환`() {
      // given
      val invalidUuid = "invalid-uuid-format"

      // when
      val response =
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .get()
          .uri("/api/v1/conversations/$invalidUuid/chat")
          .accept(MediaType.APPLICATION_JSON)
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

    @Test
    fun `페이징 파라미터 경계값 테스트`() =
      runTest {
        // given - 대화방 생성
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )

        // when - page=0, size=1로 조회
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .get()
            .uri("/api/v1/conversations/${conversation.id}/chat?page=0&size=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<List<ChatResponse>>>()
            .returnResult()
            .responseBody!!

        // then
        response.also {
          assertThat(it.statusCode).isEqualTo(200)
          assertThat(it.data).isNotNull
          assertThat(it.data?.size).isLessThanOrEqualTo(1)
        }
      }

    @Test
    fun `음수 페이징 파라미터 테스트`() =
      runTest {
        // given
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )

        // when - 음수 page로 조회 (Spring에서 내부적으로 500 에러 발생)
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .get()
            .uri("/api/v1/conversations/${conversation.id}/chat?page=-1&size=10")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody<ServiceResponse<Unit>>()
            .returnResult()
            .responseBody!!

        // then
        assertThat(response.statusCode).isEqualTo(500)
        assertThat(response.errorCode).isEqualTo("COMMON:INTERNALSERVERERROR")
        assertThat(response.message).contains("Page index must not be less than zero")
      }

    @Test
    fun `대용량 데이터 페이징 테스트`() =
      runTest {
        // given - 대화방 생성
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )

        // when - 두 번째 페이지 조회 (page=1, size=10)
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .get()
            .uri("/api/v1/conversations/${conversation.id}/chat?page=1&size=10")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<List<ChatResponse>>>()
            .returnResult()
            .responseBody!!

        // then - 페이징이 정상적으로 처리되고 빈 결과 반환
        response.also {
          assertThat(it.statusCode).isEqualTo(200)
          assertThat(it.data).isNotNull
          assertThat(it.data).isEmpty() // 테스트 환경에서는 메시지가 없으므로 빈 배열
        }
      }

    @Test
    fun `Chat 테이블 데이터로 메시지 조회 테스트`() =
      runTest {
        // given - 대화방 생성
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )

        // ASSISTANT 메시지 생성 (복합 응답)
        val assistantChatResponses =
          listOf(
            ChatResponseDto(
              type = AssistantResponseType.TEXT,
              messages = listOf("경제적 자유를 목표로 하셨군요! SMART 목표를 설정해볼까요?"),
            ),
            ChatResponseDto(
              type = AssistantResponseType.QUICK_REPLIES,
              messages = listOf("매월 300만 원 수입 💸", "빚 청산 🎯", "주식 투자 수익 목표 📈"),
            ),
          )
        val conversationId = conversation.id!!
        createTestAssistantMessage(conversationId, assistantChatResponses)

        // 시간 간격을 두기 위해 잠시 대기
        kotlinx.coroutines.delay(10)

        // USER 메시지 생성
        createTestChatMessage(conversationId, "매월 300만 원 수입 💸", ChatType.USER)

        // when - 메시지 조회
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .get()
            .uri("/api/v1/conversations/${conversation.id}/chat")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<List<ChatResponse>>>()
            .returnResult()
            .responseBody!!

        // then - 메시지 조회 결과 검증
        response.also {
          assertThat(it.statusCode).isEqualTo(200)
          assertThat(it.data).isNotNull
          assertThat(it.data).hasSize(2)

          // 첫 번째 메시지는 ASSISTANT 메시지
          val assistantMessage = it.data?.get(0)
          assertThat(assistantMessage?.conversationId).isEqualTo(conversation.id)
          assertThat(assistantMessage?.chatType?.name).isEqualTo("ASSISTANT")
          assertThat(assistantMessage?.chat).hasSize(2)
          assertThat(
            assistantMessage
              ?.chat
              ?.get(0)
              ?.type
              ?.name,
          ).isEqualTo("TEXT")
          assertThat(assistantMessage?.chat?.get(0)?.messages).hasSize(1)
          assertThat(
            assistantMessage
              ?.chat
              ?.get(0)
              ?.messages
              ?.get(0),
          ).contains("경제적 자유를 목표로 하셨군요")
          assertThat(
            assistantMessage
              ?.chat
              ?.get(1)
              ?.type
              ?.name,
          ).isEqualTo("QUICK_REPLIES")
          assertThat(assistantMessage?.chat?.get(1)?.messages).hasSize(3)
          assertThat(assistantMessage?.chat?.get(1)?.messages).contains("매월 300만 원 수입 💸", "빚 청산 🎯", "주식 투자 수익 목표 📈")

          // 두 번째 메시지는 USER 메시지 (최신순 정렬)
          val userMessage = it.data?.get(1)
          assertThat(userMessage?.conversationId).isEqualTo(conversation.id)
          assertThat(userMessage?.chatType?.name).isEqualTo("USER")
          assertThat(userMessage?.chat).hasSize(1)
          assertThat(
            userMessage
              ?.chat
              ?.get(0)
              ?.type
              ?.name,
          ).isEqualTo("TEXT")
          assertThat(userMessage?.chat?.get(0)?.messages).containsExactly("매월 300만 원 수입 💸")
        }
      }

    @Test
    fun `Chat 테이블 페이징 테스트`() =
      runTest {
        // given - 대화방 생성
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )

        // 여러 메시지 추가 (5개)
        repeat(5) { index ->
          if (index % 2 == 0) {
            // USER 메시지
            createTestChatMessage(conversation.id!!, "메시지 내용 $index", ChatType.USER)
          } else {
            // ASSISTANT 메시지
            val assistantChatResponses =
              listOf(
                ChatResponseDto(
                  type = AssistantResponseType.TEXT,
                  messages = listOf("메시지 내용 $index"),
                ),
              )
            createTestAssistantMessage(conversation.id!!, assistantChatResponses)
          }
          // 시간 간격을 두기 위해 잠시 대기
          kotlinx.coroutines.delay(10)
        }

        // when - 첫 번째 페이지 조회 (page=0, size=3)
        val firstPageResponse =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .get()
            .uri("/api/v1/conversations/${conversation.id}/chat?page=0&size=3")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<List<ChatResponse>>>()
            .returnResult()
            .responseBody!!

        // then - 첫 번째 페이지 검증
        firstPageResponse.also {
          assertThat(it.statusCode).isEqualTo(200)
          assertThat(it.data).isNotNull
          assertThat(it.data).hasSize(3)
          // 최신순 정렬이므로 마지막 3개 메시지가 조회됨
          assertThat(
            it.data
              ?.get(0)
              ?.chat
              ?.get(0)
              ?.messages
              ?.get(0),
          ).isEqualTo("메시지 내용 2")
          assertThat(
            it.data
              ?.get(1)
              ?.chat
              ?.get(0)
              ?.messages
              ?.get(0),
          ).isEqualTo("메시지 내용 3")
          assertThat(
            it.data
              ?.get(2)
              ?.chat
              ?.get(0)
              ?.messages
              ?.get(0),
          ).isEqualTo("메시지 내용 4")
        }

        // when - 두 번째 페이지 조회 (page=1, size=3)
        val secondPageResponse =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .get()
            .uri("/api/v1/conversations/${conversation.id}/chat?page=1&size=3")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<List<ChatResponse>>>()
            .returnResult()
            .responseBody!!

        // then - 두 번째 페이지 검증
        secondPageResponse.also {
          assertThat(it.statusCode).isEqualTo(200)
          assertThat(it.data).isNotNull
          assertThat(it.data).hasSize(2) // 남은 2개 메시지
          assertThat(
            it.data
              ?.get(0)
              ?.chat
              ?.get(0)
              ?.messages
              ?.get(0),
          ).isEqualTo("메시지 내용 0")
          assertThat(
            it.data
              ?.get(1)
              ?.chat
              ?.get(0)
              ?.messages
              ?.get(0),
          ).isEqualTo("메시지 내용 1")
        }
      }

    @Test
    fun `Chat 테이블 시간순 정렬 확인 테스트`() =
      runTest {
        // given - 대화방 생성
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )

        // Chat 테이블에 메시지 2개 추가하여 단순화
        val conversationId = conversation.id!!
        createTestChatMessage(conversationId, "첫 번째 메시지", ChatType.USER)
        // 시간 간격을 두기 위해 잠시 대기
        kotlinx.coroutines.delay(100)
        createTestChatMessage(conversationId, "두 번째 메시지", ChatType.USER)

        // when - 메시지 조회
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .get()
            .uri("/api/v1/conversations/${conversation.id}/chat")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<List<ChatResponse>>>()
            .returnResult()
            .responseBody!!

        // then - 메시지가 조회되고 최신순 정렬 확인
        response.also {
          assertThat(it.statusCode).isEqualTo(200)
          assertThat(it.data).isNotNull
          assertThat(it.data).hasSize(2)
          // 최신 메시지가 먼저 오도록 정렬
          assertThat(
            it.data
              ?.get(0)
              ?.chat
              ?.get(0)
              ?.messages
              ?.get(0),
          ).isEqualTo("첫 번째 메시지")
          assertThat(
            it.data
              ?.get(1)
              ?.chat
              ?.get(0)
              ?.messages
              ?.get(0),
          ).isEqualTo("두 번째 메시지")
        }
      }

    // 채팅 데이터 저장 검증 테스트들
    @Test
    fun `채팅 시작 시 AI 응답이 데이터베이스에 저장되는지 확인`() =
      runTest {
        // given & when
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/chat")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<ChatResponse>>()
            .returnResult()
            .responseBody!!

        // then - 응답 검증
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isNotNull

        // chat 테이블에 저장 검증
        val chatEntities = chatRepository.findAll().toList()
        assertThat(chatEntities).hasSize(1)

        val chatEntity = chatEntities.first()
        assertThat(chatEntity.type).isEqualTo(ChatType.ASSISTANT)
        assertThat(chatEntity.conversationId).isEqualTo(response.data?.conversationId)
        assertThat(chatEntity.content).isNotBlank()
        assertThat(chatEntity.createdAt).isNotNull()

        // JSON 내용 검증 - chat 필드는 배열 형태로 저장됨
        assertThat(chatEntity.content).contains("\"type\"")
        assertThat(chatEntity.content).contains("\"messages\"")
        assertThat(chatEntity.content).startsWith("[")
        assertThat(chatEntity.content).endsWith("]")
      }

    @Test
    fun `메시지 전송 시 사용자 요청과 AI 응답이 모두 저장되는지 확인`() =
      runTest {
        // given - 대화방 생성
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )
        val request = AssistantRequest(text = "안녕하세요! 테스트 메시지입니다.")

        mockAssistantChatResponse(conversation, request)

        // when
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/${conversation.id}/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<ChatResponse>>()
            .returnResult()
            .responseBody!!

        // then - 응답 검증
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.data).isNotNull

        // chat 테이블에 2개 레코드 저장 확인 (USER, ASSISTANT)
        val chatEntities = chatRepository.findAll().toList()
        assertThat(chatEntities).hasSize(2)

        // 타입별로 분리하여 검증
        val userMessage = chatEntities.find { it.type == ChatType.USER }
        val assistantMessage = chatEntities.find { it.type == ChatType.ASSISTANT }

        assertThat(userMessage).isNotNull
        assertThat(assistantMessage).isNotNull

        // 사용자 메시지 검증
        userMessage?.let {
          assertThat(it.conversationId).isEqualTo(conversation.id)
          assertThat(it.content).contains("안녕하세요! 테스트 메시지입니다.")
          assertThat(it.type).isEqualTo(ChatType.USER)
        }

        // AI 응답 메시지 검증
        assistantMessage?.let {
          assertThat(it.conversationId).isEqualTo(conversation.id)
          assertThat(it.content).contains("\"type\"")
          assertThat(it.content).contains("\"messages\"")
          assertThat(it.type).isEqualTo(ChatType.ASSISTANT)
        }

        // 시간순 정렬 확인 (사용자 메시지가 먼저 저장되어야 함)
        assertThat(userMessage?.createdAt).isBefore(assistantMessage?.createdAt)
      }

    @Test
    fun `저장된 채팅 데이터의 JSON 직렬화가 올바른지 확인`() =
      runTest {
        // given - 대화방 생성
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )
        val request = AssistantRequest(text = "특수문자 테스트: \"{}\", 이모지: 😀🎉, 줄바꿈\n테스트")

        mockAssistantChatResponse(conversation, request)

        // when
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/conversations/${conversation.id}/chat")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk

        // then - JSON 직렬화 검증
        val chatEntities = chatRepository.findAll().toList()
        assertThat(chatEntities).hasSize(2)

        val userMessage = chatEntities.find { it.type == ChatType.USER }
        val assistantMessage = chatEntities.find { it.type == ChatType.ASSISTANT }

        // 사용자 메시지 JSON 검증
        userMessage?.let {
          assertThat(
            it.content.isValidJson(),
          ).withFailMessage("User message content is not valid JSON: ${it.content}").isTrue()
          assertThat(it.content).contains("특수문자 테스트")
          assertThat(it.content).contains("😀🎉")
          assertThat(it.content).contains("줄바꿈")
        }

        // AI 응답 메시지 JSON 검증
        assistantMessage?.let {
          assertThat(
            it.content.isValidJson(),
          ).withFailMessage("Assistant message content is not valid JSON: ${it.content}").isTrue()
          assertThat(it.content).contains("\"type\"")
          assertThat(it.content).contains("\"messages\"")
        }
      }

    @Test
    fun `연속된 메시지 교환 시 모든 데이터가 순서대로 저장되는지 확인`() =
      runTest {
        // given - 대화방 생성
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )

        val messages =
          listOf(
            "첫 번째 메시지",
            "두 번째 메시지",
            "세 번째 메시지",
          )

        // when - 연속된 메시지 전송
        messages.forEach { messageText ->
          val request = AssistantRequest(text = messageText)
          mockAssistantChatResponse(conversation, request)

          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/conversations/${conversation.id}/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
        }

        // then - 저장된 메시지 검증
        val chatEntities = chatRepository.findAll().toList().sortedBy { it.createdAt }
        assertThat(chatEntities).hasSize(6) // 3개 메시지 × 2 (USER + ASSISTANT)

        // 메시지 순서 확인
        messages.forEachIndexed { index, expectedText ->
          val userMessageIndex = index * 2
          val assistantMessageIndex = index * 2 + 1

          // 사용자 메시지 확인
          val userMessage = chatEntities[userMessageIndex]
          assertThat(userMessage.type).isEqualTo(ChatType.USER)
          assertThat(userMessage.content).contains(expectedText)

          // AI 응답 메시지 확인
          val assistantMessage = chatEntities[assistantMessageIndex]
          assertThat(assistantMessage.type).isEqualTo(ChatType.ASSISTANT)
          assertThat(assistantMessage.content).contains("\"type\"")
          assertThat(assistantMessage.content).contains("\"messages\"")

          // 시간순 확인
          assertThat(userMessage.createdAt).isBefore(assistantMessage.createdAt)
        }

        // 전체 시간순 정렬 확인
        for (i in 0 until chatEntities.size - 1) {
          assertThat(chatEntities[i].createdAt).isBeforeOrEqualTo(chatEntities[i + 1].createdAt)
        }
      }

    // JSON 유효성 검증을 위한 헬퍼 함수
    private fun String.isValidJson(): Boolean =
      try {
        ObjectMapper()
          .readTree(this)
        true
      } catch (_: Exception) {
        false
      }

    // Chat 테이블 기반 테스트 데이터 생성 헬퍼 메서드
    private suspend fun createTestChatMessage(
      conversationId: UUID,
      content: String,
      type: ChatType,
    ): ChatEntity {
      val chatResponse =
        ChatResponse(
          conversationId = conversationId,
          chatType = type,
          chat =
            listOf(
              ChatResponseDto(
                type = AssistantResponseType.TEXT,
                messages = listOf(content),
              ),
            ),
        )
      return chatRepository.save(ChatEntity.from(chatResponse, objectMapper))
    }

    // ASSISTANT 타입 메시지 생성 헬퍼 (복합 응답 지원)
    private suspend fun createTestAssistantMessage(
      conversationId: UUID,
      chatResponses: List<ChatResponseDto>,
    ): ChatEntity {
      val chatResponse =
        ChatResponse(
          conversationId = conversationId,
          chatType = ChatType.ASSISTANT,
          chat = chatResponses,
        )
      return chatRepository.save(ChatEntity.from(chatResponse, objectMapper))
    }
  }
