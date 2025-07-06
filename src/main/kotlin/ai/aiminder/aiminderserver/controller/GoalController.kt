package ai.aiminder.aiminderserver.controller

import ai.aiminder.aiminderserver.domain.EvaluateGoalResult
import ai.aiminder.aiminderserver.dto.EvaluateGoalRequest
import ai.aiminder.aiminderserver.service.GoalService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/goal")
class GoalController(
    private val goalService: GoalService,
) {
    @PostMapping
    suspend fun evaluateGoal(
        @RequestBody
        request: EvaluateGoalRequest,
    ): EvaluateGoalResult = goalService.evaluateGoal(request)
}
