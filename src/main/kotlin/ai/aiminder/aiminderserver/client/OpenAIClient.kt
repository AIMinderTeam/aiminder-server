package ai.aiminder.aiminderserver.client

import ai.aiminder.aiminderserver.tool.GoalTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
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
import kotlin.jvm.java

@Component
class OpenAIClient(
    val chatClient: ChatClient,
    private val goalTools: GoalTools,
) {
    suspend fun request(
        systemMessage: Resource,
        userMessage: String,
        conversationId: String,
    ): Flow<String> =
        chatClient
            .prompt(Prompt(SystemMessage(systemMessage), UserMessage(userMessage)))
            .advisors { it.param(ChatMemory.CONVERSATION_ID, conversationId) }
            .tools(goalTools)
            .stream()
            .content()
            .asFlow()

    final suspend inline fun <reified T> requestStructuredResponse(
        systemMessage: Resource,
        userMessage: String,
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
            val userMessage = UserMessage(userMessage)
            val prompt = Prompt(listOf(systemMessage, userMessage), chatOptions)
            var retryCount = 0
            var response: T? = null

            while (retryCount < 5) {
                try {
                    val chatResponse = chatClient.prompt(prompt).call().chatResponse()
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
