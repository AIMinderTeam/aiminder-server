package ai.aiminder.aiminderserver.client

import ai.aiminder.aiminderserver.domain.EvaluateGoalResult
import ai.aiminder.aiminderserver.dto.EvaluateGoalRequest
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class OpenAIGoalClient(
    @Value("classpath:/prompts/goal_prompt.txt")
    private val systemPrompt: Resource,
    private val openAIClient: OpenAIClient,
) : GoalAIClient {
    override suspend fun evaluateGoal(evaluateGoalRequest: EvaluateGoalRequest): EvaluateGoalResult =
        openAIClient
            .requestStructuredResponse<EvaluateGoalResult>(
                systemMessage = systemPrompt,
                userMessage = evaluateGoalRequest.text,
            )

    override suspend fun chat(
        conversationId: String,
        userMessage: String,
    ): Flow<String> = openAIClient.request(systemPrompt, userMessage, conversationId)
}
