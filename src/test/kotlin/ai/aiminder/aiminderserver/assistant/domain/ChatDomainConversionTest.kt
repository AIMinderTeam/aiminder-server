package ai.aiminder.aiminderserver.assistant.domain

import ai.aiminder.aiminderserver.assistant.dto.ChatResponse
import ai.aiminder.aiminderserver.assistant.entity.ChatEntity
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class ChatDomainConversionTest {
  private lateinit var objectMapper: ObjectMapper

  @BeforeEach
  fun setUp() {
    objectMapper = ObjectMapper()
  }

  @Test
  fun `ChatResponse를 ChatEntity로 변환할 때 모든 필드가 올바르게 매핑되는지 확인`() =
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
                messages = listOf("테스트 메시지"),
              ),
            ),
          chatType = ChatType.USER,
        )

      // when
      val chatEntity = ChatEntity.from(chatResponse, objectMapper)

      // then
      assertThat(chatEntity.conversationId).isEqualTo(conversationId)
      assertThat(chatEntity.type).isEqualTo(ChatType.USER)
      assertThat(chatEntity.content).contains("테스트 메시지")
      assertThat(chatEntity.content).contains("TEXT")
      assertThat(chatEntity.id).isNull() // 새로운 엔티티이므로 id는 null
      assertThat(chatEntity.createdAt).isBetween(
        Instant.now().minusSeconds(1),
        Instant.now().plusSeconds(1),
      )
    }

  @Test
  fun `ChatEntity를 Chat 도메인으로 변환할 때 모든 필드가 올바르게 매핑되는지 확인`() =
    runTest {
      // given
      val conversationId = UUID.randomUUID()
      val createdAt = Instant.now()
      val chatEntity =
        ChatEntity(
          id = 1L,
          conversationId = conversationId,
          content = """[{"type":"TEXT","messages":["도메인 변환 테스트"]}]""",
          type = ChatType.ASSISTANT,
          createdAt = createdAt,
        )

      // when
      val chat = Chat.from(chatEntity)

      // then
      assertThat(chat.id).isEqualTo(1L)
      assertThat(chat.conversationId).isEqualTo(conversationId)
      assertThat(chat.content).isEqualTo(chatEntity.content)
      assertThat(chat.type).isEqualTo(ChatType.ASSISTANT)
      assertThat(chat.createdAt).isEqualTo(createdAt)
    }

  @Test
  fun `ChatEntity에서 Chat으로 변환할 때 id가 null이면 예외가 발생하는지 확인`() =
    runTest {
      // given
      // id가 null인 경우
      val chatEntity =
        ChatEntity(
          id = null,
          conversationId = UUID.randomUUID(),
          content = """[{"type":"TEXT","messages":["테스트"]}]""",
          type = ChatType.USER,
        )

      // when & then
      assertThrows<NullPointerException> {
        Chat.from(chatEntity)
      }
    }

  @Test
  fun `복잡한 ChatResponse 구조가 JSON으로 올바르게 직렬화되는지 확인`() =
    runTest {
      // given
      val chatResponse =
        ChatResponse(
          conversationId = UUID.randomUUID(),
          chat =
            listOf(
              ChatResponseDto(
                type = AssistantResponseType.TEXT,
                messages =
                  listOf(
                    "첫 번째 메시지",
                    "두 번째 메시지",
                  ),
              ),
              ChatResponseDto(
                type = AssistantResponseType.QUICK_REPLIES,
                messages =
                  listOf(
                    "옵션 1",
                    "옵션 2",
                    "옵션 3",
                  ),
              ),
            ),
          chatType = ChatType.ASSISTANT,
        )

      // when
      val chatEntity = ChatEntity.from(chatResponse, objectMapper)

      // then
      val content = chatEntity.content
      assertThat(content.isValidJson()).isTrue()

      // JSON 구조 검증
      assertThat(content).contains("\"type\":\"TEXT\"")
      assertThat(content).contains("\"type\":\"QUICK_REPLIES\"")
      assertThat(content).contains("첫 번째 메시지")
      assertThat(content).contains("두 번째 메시지")
      assertThat(content).contains("옵션 1")
      assertThat(content).contains("옵션 2")
      assertThat(content).contains("옵션 3")

      // 배열 구조 확인
      assertThat(content).startsWith("[")
      assertThat(content).endsWith("]")
      val messageCount = content.split("\"messages\":").size - 1
      assertThat(messageCount).isEqualTo(2) // 2개의 ChatResponseDto
    }

  @Test
  fun `특수문자와 이모지가 포함된 ChatResponse 변환 테스트`() =
    runTest {
      // given
      val chatResponse =
        ChatResponse(
          conversationId = UUID.randomUUID(),
          chat =
            listOf(
              ChatResponseDto(
                type = AssistantResponseType.TEXT,
                messages =
                  listOf(
                    "특수문자: !@#$%^&*()_+-={}[]|\\:;\"'<>?,./",
                    "이모지: 😀😃😄😁😆😅🥰😍🎉🎊",
                    "줄바꿈\n테스트",
                    "탭\t문자\t테스트",
                  ),
              ),
            ),
          chatType = ChatType.USER,
        )

      // when
      val chatEntity = ChatEntity.from(chatResponse, objectMapper)

      // then
      val content = chatEntity.content
      assertThat(content.isValidJson()).isTrue()
      assertThat(content).contains("!@#$%^&*()_+-=")
      assertThat(content).contains("😀😃😄😁😆😅🥰😍🎉🎊")
      assertThat(content).contains("줄바꿈")
      assertThat(content).contains("탭")
    }

  @Test
  fun `빈 chat 리스트를 가진 ChatResponse 변환 테스트`() =
    runTest {
      // given
      val chatResponse =
        ChatResponse(
          conversationId = UUID.randomUUID(),
          chat = emptyList(),
          chatType = ChatType.ASSISTANT,
        )

      // when
      val chatEntity = ChatEntity.from(chatResponse, objectMapper)

      // then
      assertThat(chatEntity.content).isEqualTo("[]")
      assertThat(chatEntity.type).isEqualTo(ChatType.ASSISTANT)
    }

  @Test
  fun `ChatType 변환이 올바르게 작동하는지 확인`() =
    runTest {
      // given
      val userChatResponse =
        ChatResponse(
          conversationId = UUID.randomUUID(),
          chat =
            listOf(
              ChatResponseDto(
                type = AssistantResponseType.TEXT,
                messages = listOf("사용자 메시지"),
              ),
            ),
          chatType = ChatType.USER,
        )

      val assistantChatResponse =
        ChatResponse(
          conversationId = UUID.randomUUID(),
          chat =
            listOf(
              ChatResponseDto(
                type = AssistantResponseType.TEXT,
                messages = listOf("어시스턴트 메시지"),
              ),
            ),
          chatType = ChatType.ASSISTANT,
        )

      // when
      val userChatEntity = ChatEntity.from(userChatResponse, objectMapper)
      val assistantChatEntity = ChatEntity.from(assistantChatResponse, objectMapper)

      // then
      assertThat(userChatEntity.type).isEqualTo(ChatType.USER)
      assertThat(assistantChatEntity.type).isEqualTo(ChatType.ASSISTANT)
    }

  @Test
  fun `라운드트립 변환에서 데이터 무결성이 유지되는지 확인`() =
    runTest {
      // given
      val originalChatResponse =
        ChatResponse(
          conversationId = UUID.randomUUID(),
          chat =
            listOf(
              ChatResponseDto(
                type = AssistantResponseType.TEXT,
                messages = listOf("라운드트립 테스트 메시지"),
              ),
              ChatResponseDto(
                type = AssistantResponseType.QUICK_REPLIES,
                messages = listOf("선택 1", "선택 2"),
              ),
            ),
          chatType = ChatType.ASSISTANT,
        )

      // when - ChatResponse → ChatEntity → Chat (saved entity simulation)
      val chatEntity = ChatEntity.from(originalChatResponse, objectMapper)
      val savedChatEntity = chatEntity.copy(id = 100L) // DB 저장 후 id 할당 시뮬레이션
      val chat = Chat.from(savedChatEntity)

      // then
      assertThat(chat.conversationId).isEqualTo(originalChatResponse.conversationId)
      assertThat(chat.type).isEqualTo(originalChatResponse.chatType)
      assertThat(chat.content).contains("라운드트립 테스트 메시지")
      assertThat(chat.content).contains("선택 1")
      assertThat(chat.content).contains("선택 2")
      assertThat(chat.content.isValidJson()).isTrue()
      assertThat(chat.id).isEqualTo(100L)
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
