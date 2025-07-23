package ai.aiminder.aiminderserver.client

import ai.aiminder.aiminderserver.domain.EvaluateGoalResult
import ai.aiminder.aiminderserver.dto.EvaluateGoalRequest
import kotlinx.coroutines.flow.Flow

interface GoalAIClient {
    suspend fun evaluateGoal(evaluateGoalRequest: EvaluateGoalRequest): EvaluateGoalResult

    suspend fun chat(
        conversationId: String,
        userMessage: String,
    ): Flow<String>
}
