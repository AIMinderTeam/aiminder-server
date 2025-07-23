package ai.aiminder.aiminderserver.controller

import ai.aiminder.aiminderserver.domain.EvaluateGoalResult
import ai.aiminder.aiminderserver.dto.EvaluateGoalRequest
import ai.aiminder.aiminderserver.service.GoalService
import kotlinx.coroutines.flow.Flow
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class GoalController(
    private val goalService: GoalService,
) {
    @PostMapping("/goal")
    suspend fun evaluateGoal(
        @RequestBody
        request: EvaluateGoalRequest,
    ): EvaluateGoalResult = goalService.evaluateGoal(request)

    @PostMapping("/chat/{conversationId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    suspend fun chat(
        @PathVariable
        conversationId: String,
        @RequestBody
        message: String,
    ): Flow<String> = goalService.chat(conversationId, message)
}
