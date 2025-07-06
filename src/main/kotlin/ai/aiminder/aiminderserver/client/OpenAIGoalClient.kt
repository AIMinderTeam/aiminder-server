package ai.aiminder.aiminderserver.client

import ai.aiminder.aiminderserver.domain.EvaluateGoalResult
import ai.aiminder.aiminderserver.dto.EvaluateGoalRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Profile("openai")
@Component
class OpenAIGoalClient(
    @Value("classpath:/prompts/goal_prompt.txt")
    private val systemPrompt: Resource,
    private val chatModel: OpenAiChatModel,
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
            val response: ChatResponse? = chatModel.call(prompt)
            val text: String = response?.result?.output?.text ?: throw IllegalStateException("결과가 존재하지 않습니다.")
            val evaluateGoalResult: EvaluateGoalResult? = outputConverter.convert(text)
            return@withContext evaluateGoalResult ?: throw IllegalStateException("결과가 존재하지 않습니다.")
        }
}
