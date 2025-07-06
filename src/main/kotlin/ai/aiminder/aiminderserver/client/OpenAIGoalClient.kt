package ai.aiminder.aiminderserver.client

import ai.aiminder.aiminderserver.domain.EvaluateGoalResult
import ai.aiminder.aiminderserver.dto.EvaluateGoalRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class OpenAIGoalClient(
    @Value("classpath:/prompts/goal_prompt.txt")
    private val systemPrompt: Resource,
    private val chatClient: ChatClient,
) : GoalAIClient {
    private val outputConverter = BeanOutputConverter(EvaluateGoalResult::class.java)
    private val chatOptions: OpenAiChatOptions? =
        OpenAiChatOptions
            .builder()
            .responseFormat(
                ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, outputConverter.jsonSchema),
            ).build()
    private val systemMessage = SystemMessage(systemPrompt)

    override suspend fun evaluateGoal(evaluateGoalRequest: EvaluateGoalRequest): EvaluateGoalResult =
        withContext(Dispatchers.Default) {
            val userMessage = UserMessage("사용자 목표: ${evaluateGoalRequest.text}")
            val prompt = Prompt(listOf(systemMessage, userMessage), chatOptions)
            var retryCount = 0
            var response: ChatResponse?
            var text: String?
            var evaluateGoalResult: EvaluateGoalResult? = null

            while (retryCount < 5) {
                try {
                    response = chatClient.prompt(prompt).call().chatResponse()
                    text = response?.result?.output?.text ?: throw IllegalStateException("결과가 존재하지 않습니다.")
                    evaluateGoalResult = outputConverter.convert(text)
                    if (evaluateGoalResult != null) {
                        break
                    }
                } catch (_: Exception) {
                    retryCount++
                    if (retryCount == 5) {
                        throw IllegalStateException("결과가 존재하지 않습니다.")
                    }
                }
            }
            return@withContext evaluateGoalResult ?: throw IllegalStateException("결과가 존재하지 않습니다.")
        }
}
