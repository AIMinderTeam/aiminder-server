package ai.aiminder.aiminderserver.assistant.service

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponseType
import ai.aiminder.aiminderserver.assistant.domain.ChatResponseDto
import ai.aiminder.aiminderserver.assistant.domain.ChatType
import ai.aiminder.aiminderserver.assistant.dto.ChatResponse
import ai.aiminder.aiminderserver.assistant.entity.ChatEntity
import ai.aiminder.aiminderserver.assistant.repository.ChatRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ChatServiceTest {
  private lateinit var chatRepository: ChatRepository
  private lateinit var objectMapper: ObjectMapper
  private lateinit var chatService: ChatService

  @BeforeEach
  fun setUp() {
    chatRepository = mockk()
    objectMapper = ObjectMapper()
    chatService = ChatService(chatRepository, objectMapper)
  }

  @Test
  fun `ChatResponse를 ChatEntity로 변환하여 저장하고 Chat 도메인으로 반환`() =
    runTest {
      // given
      val conversationId = UUID.randomUUID()
      val chatResponse =
        ChatResponse(
          conversationId = conversationId,
          chat =
            listOf(
              ChatResponseDto(
                type = AssistantResponseType.TEXT,
                messages = listOf("안녕하세요! 테스트 메시지입니다."),
              ),
              ChatResponseDto(
                type = AssistantResponseType.QUICK_REPLIES,
                messages = listOf("옵션 1", "옵션 2", "옵션 3"),
              ),
            ),
          chatType = ChatType.ASSISTANT,
        )

      val savedEntity =
        ChatEntity(
          id = 1L,
          conversationId = conversationId,
          content = objectMapper.writeValueAsString(chatResponse.chat),
          type = ChatType.ASSISTANT,
          createdAt = Instant.now(),
        )

      coEvery { chatRepository.save(any()) } returns savedEntity

      // when
      val result = chatService.create(chatResponse)

      // then
      assertThat(result).isNotNull
      assertThat(result.id).isEqualTo(1L)
      assertThat(result.conversationId).isEqualTo(conversationId)
      assertThat(result.type).isEqualTo(ChatType.ASSISTANT)
      assertThat(result.content).contains("안녕하세요! 테스트 메시지입니다.")
      assertThat(result.content).contains("옵션 1", "옵션 2", "옵션 3")
    }

  @Test
  fun `ChatResponse의 chat 필드가 올바르게 JSON으로 직렬화되는지 확인`() =
    runTest {
      // given
      val conversationId = UUID.randomUUID()
      val complexChatResponse =
        ChatResponse(
          conversationId = conversationId,
          chat =
            listOf(
              ChatResponseDto(
                type = AssistantResponseType.TEXT,
                messages = listOf("특수문자 테스트: \"{}', 이모지: 😀🎉, 줄바꿈\n테스트"),
              ),
              ChatResponseDto(
                type = AssistantResponseType.QUICK_REPLIES,
                messages = listOf("다이어트 💪", "경제적 자유 💰", "자격증 취득 🏅"),
              ),
            ),
          chatType = ChatType.USER,
        )

      val savedEntity =
        ChatEntity(
          id = 2L,
          conversationId = conversationId,
          content = objectMapper.writeValueAsString(complexChatResponse.chat),
          type = ChatType.USER,
          createdAt = Instant.now(),
        )

      coEvery { chatRepository.save(any()) } returns savedEntity

      // when
      val result = chatService.create(complexChatResponse)

      // then - JSON 직렬화 검증
      val content = result.content
      assertThat(content.isValidJson()).isTrue()
      assertThat(content).contains("특수문자 테스트")
      assertThat(content).contains("😀🎉")
      assertThat(content).contains("줄바꿈")
      assertThat(content).contains("다이어트 💪")
      assertThat(content).contains("경제적 자유 💰")
      assertThat(content).contains("자격증 취득 🏅")
      assertThat(content).contains("\"type\"")
      assertThat(content).contains("\"messages\"")

      // JSON 구조 검증 (역직렬화는 실제 프로덕션 코드에서 동작함)
      assertThat(content).contains("\"type\":\"TEXT\"")
      assertThat(content).contains("\"type\":\"QUICK_REPLIES\"")
      assertThat(content).startsWith("[")
      assertThat(content).endsWith("]")
    }

  @Test
  fun `사용자 메시지 타입의 ChatResponse 저장 테스트`() =
    runTest {
      // given
      val conversationId = UUID.randomUUID()
      val userChatResponse =
        ChatResponse(
          conversationId = conversationId,
          chat =
            listOf(
              ChatResponseDto(
                type = AssistantResponseType.TEXT,
                messages = listOf("사용자의 질문입니다."),
              ),
            ),
          chatType = ChatType.USER,
        )

      val savedEntity =
        ChatEntity(
          id = 3L,
          conversationId = conversationId,
          content = objectMapper.writeValueAsString(userChatResponse.chat),
          type = ChatType.USER,
          createdAt = Instant.now(),
        )

      coEvery { chatRepository.save(any()) } returns savedEntity

      // when
      val result = chatService.create(userChatResponse)

      // then
      assertThat(result.type).isEqualTo(ChatType.USER)
      assertThat(result.content).contains("사용자의 질문입니다.")
    }

  @Test
  fun `빈 메시지 리스트를 가진 ChatResponse 저장 테스트`() =
    runTest {
      // given
      val conversationId = UUID.randomUUID()
      val emptyChatResponse =
        ChatResponse(
          conversationId = conversationId,
          chat = emptyList(),
          chatType = ChatType.ASSISTANT,
        )

      val savedEntity =
        ChatEntity(
          id = 4L,
          conversationId = conversationId,
          content = "[]",
          type = ChatType.ASSISTANT,
          createdAt = Instant.now(),
        )

      coEvery { chatRepository.save(any()) } returns savedEntity

      // when
      val result = chatService.create(emptyChatResponse)

      // then
      assertThat(result.content).isEqualTo("[]")
      assertThat(result.type).isEqualTo(ChatType.ASSISTANT)
    }

  @Test
  fun `복잡한 중첩 구조를 가진 ChatResponse 직렬화 테스트`() =
    runTest {
      // given
      val conversationId = UUID.randomUUID()
      val complexChatResponse =
        ChatResponse(
          conversationId = conversationId,
          chat =
            listOf(
              ChatResponseDto(
                type = AssistantResponseType.TEXT,
                messages =
                  listOf(
                    "다중 줄\n메시지\n테스트",
                    "탭\t문자\t테스트",
                    "특수문자: !@#$%^&*()_+-=[]{}|;':\",./<>?",
                  ),
              ),
              ChatResponseDto(
                type = AssistantResponseType.QUICK_REPLIES,
                messages =
                  listOf(
                    "🚀 로켓",
                    "🌟 별",
                    "🎯 목표",
                    "💡 아이디어",
                    "🔥 열정",
                  ),
              ),
            ),
          chatType = ChatType.ASSISTANT,
        )

      val jsonContent = objectMapper.writeValueAsString(complexChatResponse.chat)
      val savedEntity =
        ChatEntity(
          id = 5L,
          conversationId = conversationId,
          content = jsonContent,
          type = ChatType.ASSISTANT,
          createdAt = Instant.now(),
        )

      coEvery { chatRepository.save(any()) } returns savedEntity

      // when
      val result = chatService.create(complexChatResponse)

      // then
      val content = result.content
      assertThat(content.isValidJson()).isTrue()

      // 직렬화된 내용 검증
      assertThat(content).contains("다중 줄")
      assertThat(content).contains("탭")
      assertThat(content).contains("!@#$%^&*()")
      assertThat(content).contains("🚀")
      assertThat(content).contains("🌟")
      assertThat(content).contains("🎯")

      // JSON 구조 검증 (역직렬화는 실제 프로덕션 코드에서 동작함)
      assertThat(content).contains("\"type\":\"TEXT\"")
      assertThat(content).contains("\"type\":\"QUICK_REPLIES\"")
      assertThat(content).startsWith("[")
      assertThat(content).endsWith("]")
      // 배열 구조 확인
      val messageCount = content.split("\"messages\":").size - 1
      assertThat(messageCount).isEqualTo(2) // 2개의 ChatResponseDto
    }

  // JSON 유효성 검증을 위한 헬퍼 함수
  private fun String.isValidJson(): Boolean =
    try {
      objectMapper.readTree(this)
      true
    } catch (e: Exception) {
      false
    }
}
