package ai.aiminder.aiminderserver.assistant.client

import ai.aiminder.aiminderserver.assistant.tool.GoalTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.time.LocalDateTime.now
import java.util.UUID
import kotlin.jvm.java

@Component
class OpenAIClient(
  val chatClient: ChatClient,
  val goalTool: GoalTool,
) {
  final suspend inline fun <reified T> requestStructuredResponse(
    systemMessage: Resource,
    userMessage: String,
    conversationId: UUID,
  ): T =
    withContext(Dispatchers.Default) {
      val logger = LoggerFactory.getLogger(this::class.java)
      val outputConverter = BeanOutputConverter(T::class.java)
      val chatOptions: OpenAiChatOptions =
        OpenAiChatOptions
          .builder()
          .responseFormat(ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, outputConverter.jsonSchema))
          .build()
      val systemMessage = SystemMessage(systemMessage)
      val userMessage = UserMessage("Current Time: ${now()}\n$userMessage")
      val prompt = Prompt(listOf(systemMessage, userMessage), chatOptions)
      var retryCount = 0
      var response: T? = null

      while (retryCount < 5) {
        try {
          val chatResponse =
            chatClient
              .prompt(
                prompt,
              ).advisors { it.param(ChatMemory.CONVERSATION_ID, conversationId) }
              .tools(goalTool)
              .call()
              .chatResponse()
          val text =
            chatResponse?.result?.output?.text
              ?: throw IllegalAccessException("결과가 존재하지 않습니다.")
          response = outputConverter.convert(text)
            ?: throw IllegalAccessException("변환을 실패했습니다.")
          return@withContext response
        } catch (exception: Exception) {
          retryCount++
          if (retryCount == 5) {
            logger.error("AI 요청을 실패했습니다.", exception)
            throw IllegalAccessException("AI 요청을 실패했습니다.")
          }
          logger.error("AI 재요청을 수행합니다.", exception)
        }
      }
      return@withContext response ?: throw IllegalAccessException("AI 요청을 실패했습니다")
    }
}