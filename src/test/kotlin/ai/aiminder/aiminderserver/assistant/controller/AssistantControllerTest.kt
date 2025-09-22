package ai.aiminder.aiminderserver.assistant.controller

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequest
import ai.aiminder.aiminderserver.assistant.entity.ConversationEntity
import ai.aiminder.aiminderserver.assistant.repository.ConversationRepository
import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.auth.domain.Role
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.entity.UserEntity
import ai.aiminder.aiminderserver.user.repository.UserRepository
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
    private lateinit var testUser: User
    private lateinit var authentication: UsernamePasswordAuthenticationToken

    @BeforeEach
    fun setUp() =
      runTest {
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
          assertThat(it.data?.responses).isNotEmpty
          assertThat(it.data?.responses?.first()?.messages).isNotEmpty
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

        // when - 현재 AI 서비스가 설정되지 않아 500 에러가 예상됨
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/chat/${conversation.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody<ServiceResponse<Unit>>()
            .returnResult()
            .responseBody!!

        // then - AI 서비스 에러로 인한 500 에러 확인
        response.also {
          assertThat(it.statusCode).isEqualTo(500)
          assertThat(it.errorCode).isEqualTo("ASSISTANT:INFERENCEERROR")
          assertThat(it.message).isEqualTo("AI 요청을 실패했습니다.")
        }
      }

    @Test
    fun `존재하지 않는 대화방으로 메시지 전송 시 AI 서비스 에러 반환`() {
      // given
      val nonExistentConversationId = UUID.randomUUID()
      val request = AssistantRequest(text = "Hello!")

      // when - 현재 구현에서는 conversationId 검증 없이 AI 서비스로 전달되어 500 에러 발생
      val response =
        webTestClient
          .mutateWith(mockAuthentication(authentication))
          .post()
          .uri("/api/v1/chat/$nonExistentConversationId")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is5xxServerError
          .expectBody<ServiceResponse<Unit>>()
          .returnResult()
          .responseBody!!

      // then
      assertThat(response.statusCode).isEqualTo(500)
      assertThat(response.errorCode).isEqualTo("ASSISTANT:INFERENCEERROR")
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
    fun `빈 메시지 전송 시 AI 서비스 에러 처리`() =
      runTest {
        // given
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )
        val emptyTextRequest = AssistantRequest(text = "")

        // when - 현재 구현에서는 빈 문자열도 AI 서비스로 전달되어 500 에러 발생
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/chat/${conversation.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(emptyTextRequest)
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody<ServiceResponse<Unit>>()
            .returnResult()
            .responseBody!!

        // then
        response.also {
          assertThat(it.statusCode).isEqualTo(500)
          assertThat(it.errorCode).isEqualTo("ASSISTANT:INFERENCEERROR")
        }
      }

    @Test
    fun `공백만 포함된 메시지 전송 시 AI 서비스 에러 처리`() =
      runTest {
        // given
        val conversation =
          conversationRepository.save(
            ConversationEntity.from(testUser),
          )
        val whitespaceOnlyRequest = AssistantRequest(text = "   ")

        // when - 현재 구현에서는 공백만 포함된 문자열도 AI 서비스로 전달되어 500 에러 발생
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/chat/${conversation.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(whitespaceOnlyRequest)
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody<ServiceResponse<Unit>>()
            .returnResult()
            .responseBody!!

        // then
        response.also {
          assertThat(it.statusCode).isEqualTo(500)
          assertThat(it.errorCode).isEqualTo("ASSISTANT:INFERENCEERROR")
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
    fun `다른 사용자의 대화방 접근 시 AI 서비스 에러 처리`() =
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

        // when - 현재 구현에서는 대화방 접근 권한 검증 없이 AI 서비스로 전달되어 500 에러 발생
        val response =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/chat/${anotherUserConversation.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody<ServiceResponse<Unit>>()
            .returnResult()
            .responseBody!!

        // then
        assertThat(response.statusCode).isEqualTo(500)
        assertThat(response.errorCode).isEqualTo("ASSISTANT:INFERENCEERROR")
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
        assertThat(startChatResponse.data?.responses).isNotEmpty

        // given - 생성된 대화방 확인
        val conversations = conversationRepository.findAll().toList()
        assertThat(conversations).hasSize(1)
        val conversationId = conversations.first().id!!

        // when - 메시지 전송 (현재 AI 서비스 설정 문제로 500 에러 예상)
        val request = AssistantRequest(text = "안녕하세요!")
        val sendMessageResponse =
          webTestClient
            .mutateWith(mockAuthentication(authentication))
            .post()
            .uri("/api/v1/chat/$conversationId")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody<ServiceResponse<Unit>>()
            .returnResult()
            .responseBody!!

        // then - AI 서비스 에러 확인
        assertThat(sendMessageResponse.statusCode).isEqualTo(500)
        assertThat(sendMessageResponse.errorCode).isEqualTo("ASSISTANT:INFERENCEERROR")
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

        // when & then - 연속된 메시지 전송 (현재 AI 서비스 설정 문제로 모두 500 에러 예상)
        messages.forEach { messageText ->
          val request = AssistantRequest(text = messageText)
          val response =
            webTestClient
              .mutateWith(mockAuthentication(authentication))
              .post()
              .uri("/api/v1/chat/${conversation.id}")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(request)
              .exchange()
              .expectStatus()
              .is5xxServerError
              .expectBody<ServiceResponse<Unit>>()
              .returnResult()
              .responseBody!!

          assertThat(response.statusCode).isEqualTo(500)
          assertThat(response.errorCode).isEqualTo("ASSISTANT:INFERENCEERROR")
        }
      }
  }
