package ai.aiminder.aiminderserver.service

import ai.aiminder.aiminderserver.client.GoalAIClient
import ai.aiminder.aiminderserver.domain.EvaluateGoalResult
import ai.aiminder.aiminderserver.dto.EvaluateGoalRequest
import org.springframework.stereotype.Service

@Service
class GoalService(
    private val goalAIClient: GoalAIClient,
) {
    suspend fun evaluateGoal(request: EvaluateGoalRequest): EvaluateGoalResult = goalAIClient.evaluateGoal(request)
}
