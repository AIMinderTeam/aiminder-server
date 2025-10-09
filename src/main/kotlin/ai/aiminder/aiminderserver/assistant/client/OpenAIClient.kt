package ai.aiminder.aiminderserver.assistant.client

import ai.aiminder.aiminderserver.assistant.dto.AssistantRequestDto
import ai.aiminder.aiminderserver.assistant.error.AssistantError
import ai.aiminder.aiminderserver.assistant.service.ToolContextService
import ai.aiminder.aiminderserver.assistant.tool.AssistantTool
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

@Component
class OpenAIClient(
  val chatClient: ChatClient,
  val toolContextService: ToolContextService,
) {
  final suspend inline fun <reified T> requestStructuredResponse(
    dto: AssistantRequestDto,
    systemMessage: Resource,
    tools: List<AssistantTool>,
  ): T =
    withContext(Dispatchers.Default) {
      val logger = LoggerFactory.getLogger(this::class.java)
      val outputConverter = BeanOutputConverter(T::class.java)
      val chatOptions: OpenAiChatOptions =
        OpenAiChatOptions
          .builder()
          .responseFormat(ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, outputConverter.jsonSchema))
          .build()
      val now = "현재 시간: ${now()}\n"
      val systemMessage = SystemMessage(now + systemMessage.getContentAsString(Charsets.UTF_8))
      val userMessage = UserMessage(dto.text)
      val prompt = Prompt(listOf(systemMessage, userMessage), chatOptions)
      var response: T?
      val toolContext: Map<String, UUID> = toolContextService.create(dto.conversationId, dto.userId)

      try {
        val chatResponse =
          chatClient
            .prompt(
              prompt,
            ).advisors { it.param(ChatMemory.CONVERSATION_ID, dto.conversationId) }
            .toolContext(toolContext)
            .tools(tools)
            .call()
            .chatResponse()
        val text =
          chatResponse?.result?.output?.text
            ?: throw AssistantError.InferenceError("AI 결과가 존재하지 않습니다.")
        response = outputConverter.convert(text)
          ?: throw AssistantError.InferenceError("AI 변환을 실패했습니다.")
        return@withContext response
      } catch (exception: Exception) {
        logger.error("AI 요청을 실패했습니다.", exception)
        throw AssistantError.InferenceError("AI 요청을 실패했습니다.")
      }
    }
}
