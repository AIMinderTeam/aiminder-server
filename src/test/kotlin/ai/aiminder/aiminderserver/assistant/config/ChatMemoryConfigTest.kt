package ai.aiminder.aiminderserver.assistant.config

import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestPropertySource(
  properties = [
    "spring.ai.openai.api-key=test-key",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
  ],
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ChatMemoryConfigTest.TestDataSourceConfig::class)
class ChatMemoryConfigTest : BaseIntegrationTest() {
  @Autowired
  private lateinit var chatMemory: ChatMemory

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Test
  @DisplayName("채팅 메모리가 데이터베이스에 올바르게 저장되고 조회되는지 확인")
  fun `should persist and retrieve chat memory from database`() {
    // given
    val conversationId = UUID.randomUUID().toString()
    val userMessage = UserMessage("안녕하세요, 테스트 메시지입니다.")

    // when - 메시지 저장
    chatMemory.add(conversationId, userMessage)

    // then - 데이터베이스에서 직접 확인
    val count =
      jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM spring_ai_chat_memory WHERE conversation_id = ?",
        Int::class.java,
        conversationId,
      )
    assertEquals(1, count)

    // 메시지 내용 확인
    val savedContent =
      jdbcTemplate.queryForObject(
        "SELECT content FROM spring_ai_chat_memory WHERE conversation_id = ? AND message_index = 1",
        String::class.java,
        conversationId,
      )
    assertTrue(savedContent?.contains("안녕하세요, 테스트 메시지입니다.") == true)
  }

  @Test
  @DisplayName("채팅 메모리 윈도우 크기가 20개로 제한되는지 확인")
  fun `should limit chat memory window to 20 messages`() {
    // given
    val conversationId = UUID.randomUUID().toString()

    // when - 25개 메시지 추가
    repeat(25) { index ->
      val userMessage = UserMessage("테스트 메시지 $index")
      chatMemory.add(conversationId, userMessage)
    }

    // then - 데이터베이스에 20개만 남아있는지 확인
    val count =
      jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM spring_ai_chat_memory WHERE conversation_id = ?",
        Int::class.java,
        conversationId,
      )
    assertEquals(20, count)

    // 메모리에서 조회시에도 20개만 반환되는지 확인
    val messages = chatMemory.get(conversationId) // 메모리에서 조회
    assertEquals(20, messages.size) // 20개만 반환
  }

  @Test
  @DisplayName("다른 대화 세션간 메시지가 격리되는지 확인")
  fun `should isolate messages between different conversation sessions`() {
    // given
    val conversationId1 = UUID.randomUUID().toString()
    val conversationId2 = UUID.randomUUID().toString()

    // when
    chatMemory.add(conversationId1, UserMessage("첫 번째 대화"))
    chatMemory.add(conversationId2, UserMessage("두 번째 대화"))

    // then
    val messages1 = chatMemory.get(conversationId1)
    val messages2 = chatMemory.get(conversationId2)

    assertEquals(1, messages1.size)
    assertEquals(1, messages2.size)

    assertTrue(messages1[0].text.contains("첫 번째 대화"))
    assertTrue(messages2[0].text.contains("두 번째 대화"))
  }

  @Test
  @DisplayName("채팅 메모리 클리어가 정상 동작하는지 확인")
  fun `should clear chat memory correctly`() {
    // given
    val conversationId = UUID.randomUUID().toString()
    chatMemory.add(conversationId, UserMessage("테스트 메시지"))

    // when
    chatMemory.clear(conversationId)

    // then
    val count =
      jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM spring_ai_chat_memory WHERE conversation_id = ?",
        Int::class.java,
        conversationId,
      )
    assertEquals(0, count)

    val messages = chatMemory.get(conversationId)
    assertTrue(messages.isEmpty())
  }

  @TestConfiguration
  class TestDataSourceConfig {
    @Bean
    fun dataSource(environment: Environment): DataSource {
      val jdbcUrl = environment.getProperty("spring.datasource.url")!!
      val username = environment.getProperty("spring.datasource.username")!!
      val password = environment.getProperty("spring.datasource.password")!!
      val driverClassName = environment.getProperty("spring.datasource.driver-class-name")!!

      return DataSourceBuilder
        .create()
        .url(jdbcUrl)
        .username(username)
        .password(password)
        .driverClassName(driverClassName)
        .build()
    }
  }
}
