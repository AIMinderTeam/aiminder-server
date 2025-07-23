package ai.aiminder.aiminderserver.service

import ai.aiminder.aiminderserver.client.GoalAIClient
import ai.aiminder.aiminderserver.domain.EvaluateGoalResult
import ai.aiminder.aiminderserver.dto.EvaluateGoalRequest
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class GoalService(
    private val goalAIClient: GoalAIClient,
) {
    suspend fun evaluateGoal(request: EvaluateGoalRequest): EvaluateGoalResult = goalAIClient.evaluateGoal(request)

    suspend fun chat(
        conversationId: String,
        userMessage: String,
    ): Flow<String> = goalAIClient.chat(conversationId, userMessage)
}
