package ai.aiminder.aiminderserver.assistant.repository

import ai.aiminder.aiminderserver.assistant.domain.ChatType
import ai.aiminder.aiminderserver.assistant.entity.ChatEntity
import ai.aiminder.aiminderserver.auth.domain.OAuth2Provider
import ai.aiminder.aiminderserver.common.BaseIntegrationTest
import ai.aiminder.aiminderserver.conversation.entity.ConversationEntity
import ai.aiminder.aiminderserver.conversation.repository.ConversationRepository
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.entity.UserEntity
import ai.aiminder.aiminderserver.user.repository.UserRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class ChatRepositoryTest
  @Autowired
  constructor(
    private val chatRepository: ChatRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
  ) : BaseIntegrationTest() {
    private lateinit var testUser: User
    private lateinit var testConversation: ConversationEntity

    @BeforeEach
    fun setUp() =
      runTest {
        // 테스트용 사용자 생성
        val userEntity =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.GOOGLE,
              providerId = "test-chat-repository-user",
            ),
          )
        testUser = User.from(userEntity)

        // 테스트용 대화방 생성
        testConversation = conversationRepository.save(ConversationEntity.from(testUser))
      }

    @Test
    fun `ChatRepository 기본 CRUD 동작 확인`() =
      runTest {
        // given
        val chatEntity =
          ChatEntity(
            conversationId = testConversation.id!!,
            content = """[{"type":"TEXT","messages":["테스트 메시지입니다."]}]""",
            type = ChatType.USER,
            createdAt = Instant.now(),
          )

        // when - save
        val savedEntity = chatRepository.save(chatEntity)

        // then - save 검증
        assertThat(savedEntity.id).isNotNull()
        assertThat(savedEntity.conversationId).isEqualTo(testConversation.id)
        assertThat(savedEntity.content).contains("테스트 메시지입니다.")
        assertThat(savedEntity.type).isEqualTo(ChatType.USER)
        assertThat(savedEntity.createdAt).isNotNull()

        // when - findById
        val foundEntity = chatRepository.findById(savedEntity.id!!)

        // then - findById 검증
        assertThat(foundEntity).isNotNull()
        assertThat(foundEntity?.conversationId).isEqualTo(testConversation.id)
        assertThat(foundEntity?.content).isEqualTo(chatEntity.content)
        assertThat(foundEntity?.type).isEqualTo(ChatType.USER)

        // when - existsById
        val exists = chatRepository.existsById(savedEntity.id!!)

        // then - existsById 검증
        assertThat(exists).isTrue()

        // when - deleteById
        chatRepository.deleteById(savedEntity.id!!)

        // then - delete 검증
        val deletedEntity = chatRepository.findById(savedEntity.id!!)
        assertThat(deletedEntity).isNull()
        assertThat(chatRepository.existsById(savedEntity.id!!)).isFalse()
      }

    @Test
    fun `다양한 ChatType의 엔티티 저장 및 조회 테스트`() =
      runTest {
        // given
        // conversationId는 testConversation.id 사용
        val userChatEntity =
          ChatEntity(
            conversationId = testConversation.id!!,
            content = """[{"type":"TEXT","messages":["사용자 메시지"]}]""",
            type = ChatType.USER,
          )

        val assistantChatEntity =
          ChatEntity(
            conversationId = testConversation.id!!,
            content = """[{"type":"TEXT","messages":["AI 응답 메시지"]}]""",
            type = ChatType.ASSISTANT,
          )

        // when
        val savedUserEntity = chatRepository.save(userChatEntity)
        val savedAssistantEntity = chatRepository.save(assistantChatEntity)

        // then
        assertThat(savedUserEntity.type).isEqualTo(ChatType.USER)
        assertThat(savedAssistantEntity.type).isEqualTo(ChatType.ASSISTANT)
        assertThat(savedUserEntity.content).contains("사용자 메시지")
        assertThat(savedAssistantEntity.content).contains("AI 응답 메시지")
      }

    @Test
    fun `대용량 JSON 컨텐츠 저장 및 조회 테스트`() =
      runTest {
        // given
        // conversationId는 testConversation.id 사용
        val largeContent =
          """
          [
            {
              "type": "TEXT",
              "messages": [
                "매우 긴 메시지입니다. ".repeat(100) + "끝"
              ]
            },
            {
              "type": "QUICK_REPLIES",
              "messages": [
                "옵션 1",
                "옵션 2",
                "옵션 3",
                "옵션 4",
                "옵션 5"
              ]
            }
          ]
          """.trimIndent()

        val chatEntity =
          ChatEntity(
            conversationId = testConversation.id!!,
            content = largeContent,
            type = ChatType.ASSISTANT,
          )

        // when
        val savedEntity = chatRepository.save(chatEntity)

        // then
        assertThat(savedEntity.content).hasSize(largeContent.length)
        assertThat(savedEntity.content).contains("매우 긴 메시지입니다.")
        assertThat(savedEntity.content).contains("옵션 1")
        assertThat(savedEntity.content).contains("끝")
      }

    @Test
    fun `특수문자와 이모지가 포함된 컨텐츠 저장 테스트`() =
      runTest {
        // given
        // conversationId는 testConversation.id 사용
        val specialContent =
          """
          [
            {
              "type": "TEXT",
              "messages": [
                "특수문자: !@#$%^&*()_+-={}|[]\\:;\"'<>?,./",
                "이모지: 😀😃😄😁😆😅😂🤣🥲☺️😊😇🙂🙃😉😌😍🥰😘😗😙😚😋😛😝",
                "줄바꿈\n테스트\n입니다.",
                "탭\t문자\t테스트"
              ]
            }
          ]
          """.trimIndent()

        val chatEntity =
          ChatEntity(
            conversationId = testConversation.id!!,
            content = specialContent,
            type = ChatType.USER,
          )

        // when
        val savedEntity = chatRepository.save(chatEntity)
        val foundEntity = chatRepository.findById(savedEntity.id!!)

        // then
        assertThat(foundEntity).isNotNull()
        assertThat(foundEntity?.content).contains("!@#$%^&*()")
        assertThat(foundEntity?.content).contains("😀😃😄")
        assertThat(foundEntity?.content).contains("줄바꿈")
        assertThat(foundEntity?.content).contains("탭")
      }

    @Test
    fun `동일한 conversationId로 여러 메시지 저장 테스트`() =
      runTest {
        // given
        // conversationId는 testConversation.id 사용
        val messages =
          listOf(
            "첫 번째 메시지",
            "두 번째 메시지",
            "세 번째 메시지",
            "네 번째 메시지",
            "다섯 번째 메시지",
          )

        val chatEntities =
          messages.mapIndexed { index, message ->
            ChatEntity(
              conversationId = testConversation.id!!,
              content = """[{"type":"TEXT","messages":["$message"]}]""",
              type = if (index % 2 == 0) ChatType.USER else ChatType.ASSISTANT,
              createdAt = Instant.now().plusMillis(index.toLong()),
            )
          }

        // when
        val savedEntities = chatEntities.map { chatRepository.save(it) }

        // then
        assertThat(savedEntities).hasSize(5)
        savedEntities.forEachIndexed { index, entity ->
          assertThat(entity.conversationId).isEqualTo(testConversation.id!!)
          assertThat(entity.content).contains(messages[index])
          assertThat(entity.type).isEqualTo(if (index % 2 == 0) ChatType.USER else ChatType.ASSISTANT)
        }

        // 모든 엔티티가 저장되었는지 확인
        val allEntities = chatRepository.findAll().toList()
        val conversationEntities = allEntities.filter { it.conversationId == testConversation.id!! }
        assertThat(conversationEntities).hasSize(5)
      }

    @Test
    fun `findAll으로 모든 채팅 메시지 조회 테스트`() =
      runTest {
        // given
        // 추가 대화방 생성
        val additionalUser =
          userRepository.save(
            UserEntity(
              provider = OAuth2Provider.GOOGLE,
              providerId = "test-additional-user",
            ),
          )
        val additionalConversation =
          conversationRepository.save(ConversationEntity.from(User.from(additionalUser)))

        val chat1 =
          ChatEntity(
            conversationId = testConversation.id!!,
            content = """[{"type":"TEXT","messages":["대화방 1의 메시지"]}]""",
            type = ChatType.USER,
          )

        val chat2 =
          ChatEntity(
            conversationId = additionalConversation.id!!,
            content = """[{"type":"TEXT","messages":["대화방 2의 메시지"]}]""",
            type = ChatType.ASSISTANT,
          )

        // when
        chatRepository.save(chat1)
        chatRepository.save(chat2)
        val allChats = chatRepository.findAll().toList()

        // then
        assertThat(allChats).hasSizeGreaterThanOrEqualTo(2)
        val conversation1Chats = allChats.filter { it.conversationId == testConversation.id!! }
        val conversation2Chats = allChats.filter { it.conversationId == additionalConversation.id!! }

        assertThat(conversation1Chats).hasSize(1)
        assertThat(conversation2Chats).hasSize(1)
        assertThat(conversation1Chats.first().content).contains("대화방 1의 메시지")
        assertThat(conversation2Chats.first().content).contains("대화방 2의 메시지")
      }

    @Test
    fun `count와 deleteAll 동작 테스트`() =
      runTest {
        // given
        // conversationId는 testConversation.id 사용
        val chat1 =
          ChatEntity(
            conversationId = testConversation.id!!,
            content = """[{"type":"TEXT","messages":["메시지 1"]}]""",
            type = ChatType.USER,
          )
        val chat2 =
          ChatEntity(
            conversationId = testConversation.id!!,
            content = """[{"type":"TEXT","messages":["메시지 2"]}]""",
            type = ChatType.ASSISTANT,
          )

        // when
        val initialCount = chatRepository.count()
        chatRepository.save(chat1)
        chatRepository.save(chat2)
        val afterSaveCount = chatRepository.count()

        // then
        assertThat(afterSaveCount).isEqualTo(initialCount + 2)

        // when - deleteAll
        chatRepository.deleteAll()
        val finalCount = chatRepository.count()

        // then
        assertThat(finalCount).isEqualTo(0)
      }
  }
