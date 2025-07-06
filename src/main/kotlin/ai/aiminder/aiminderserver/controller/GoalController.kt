package ai.aiminder.aiminderserver.controller

import ai.aiminder.aiminderserver.domain.EvaluateGoalResult
import ai.aiminder.aiminderserver.dto.EvaluateGoalRequest
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.withContext
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/goal")
class GoalController(
    @Value("classpath:/prompts/goal_prompt.txt")
    private val systemPrompt: Resource,
    private val chatClient: ChatClient,
) {
    private val outputConverter = BeanOutputConverter(EvaluateGoalResult::class.java)
    private val chatOptions: OllamaOptions? = OllamaOptions.builder().format(outputConverter.jsonSchemaMap).build()
    private val systemMessage = SystemMessage(systemPrompt)

    @PostMapping
    suspend fun evaluateGoal(
        @RequestBody
        request: EvaluateGoalRequest,
    ): EvaluateGoalResult =
        withContext(Default) {
            val systemMessage = systemMessage
            repeat(5) { attempt ->
                val userMessage = UserMessage("사용자 목표: " + request.text)
                val prompt = Prompt(listOf(systemMessage, userMessage), chatOptions)
                val response: ChatResponse? = chatClient.prompt(prompt).call().chatResponse()
                val text: String = response?.result?.output?.text ?: throw IllegalStateException("결과가 존재하지 않습니다.")
                try {
                    val evaluateGoalResult: EvaluateGoalResult? = outputConverter.convert(text)
                    return@withContext evaluateGoalResult ?: throw IllegalStateException("결과가 존재하지 않습니다.")
                } catch (e: Exception) {
                    if (attempt == 4) {
                        throw e
                    }
                }
            }
            throw IllegalStateException("모든 재시도가 실패했습니다.")
        }
}
