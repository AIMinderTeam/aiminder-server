package ai.aiminder.aiminderserver.goal.controller

import ai.aiminder.aiminderserver.common.error.Response
import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequest
import ai.aiminder.aiminderserver.goal.service.GoalService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/goal")
class GoalController(
  private val goalService: GoalService,
) {
  @PostMapping
  suspend fun createGoal(
    @RequestBody
    request: CreateGoalRequest,
  ): Response<Goal> =
    goalService
      .create(request)
      .let { goal -> Response.from(goal) }
}