package ai.aiminder.aiminderserver.assistant.controller

import ai.aiminder.aiminderserver.assistant.client.AssistantClient
import ai.aiminder.aiminderserver.assistant.domain.AssistantResponseDto
import ai.aiminder.aiminderserver.assistant.domain.AssistantResponsePayload
import ai.aiminder.aiminderserver.assistant.domain.AssistantResponseType
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequest
import ai.aiminder.aiminderserver.assistant.dto.AssistantResponse
import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.conversation.entity.ConversationEntity
import ai.aiminder.aiminderserver.conversation.repository.ConversationRepository
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.entity.UserEntity
import ai.aiminder.aiminderserver.user.repository.UserRepository
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
  ) : BaseIntegrationTest() {
    @MockkBean
    private lateinit var assistantClient: AssistantClient
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
            .uri("/api/v1/chat")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<AssistantResponse>>()
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
          .uri("/api/v1/chat")
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
          .uri("/api/v1/chat")
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
            .uri("/api/v1/chat/${conversation.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<AssistantResponse>>()
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
        AssistantResponseDto(
          responses =
            listOf(
              AssistantResponsePayload(
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
          .uri("/api/v1/chat/$nonExistentConversationId")
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
            .uri("/api/v1/chat/${conversation.id}")
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
            .uri("/api/v1/chat/${conversation.id}")
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
            .uri("/api/v1/chat/${conversation.id}")
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
            .uri("/api/v1/chat/${conversation.id}")
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
            .uri("/api/v1/chat/${anotherUserConversation.id}")
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
            .uri("/api/v1/chat")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<AssistantResponse>>()
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
            .uri("/api/v1/chat/$conversationId")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<ServiceResponse<AssistantResponse>>()
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
              .uri("/api/v1/chat/${conversation.id}")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(request)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody<ServiceResponse<AssistantResponse>>()
              .returnResult()
              .responseBody!!

          assertThat(response.statusCode).isEqualTo(200)
          assertThat(response.data).isNotNull
        }
      }
  }
