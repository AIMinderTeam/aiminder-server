package ai.aiminder.aiminderserver.client

import ai.aiminder.aiminderserver.domain.EvaluateGoalResult
import ai.aiminder.aiminderserver.dto.EvaluateGoalRequest
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
            .call<EvaluateGoalResult>(
                systemMessage = systemPrompt,
                userMessage = evaluateGoalRequest.text,
            )
}
