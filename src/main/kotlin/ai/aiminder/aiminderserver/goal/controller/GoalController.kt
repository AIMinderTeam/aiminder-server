package ai.aiminder.aiminderserver.goal.controller

import ai.aiminder.aiminderserver.auth.domain.User
import ai.aiminder.aiminderserver.common.error.ServiceResponse
import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequest
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequestDto
import ai.aiminder.aiminderserver.goal.service.GoalService
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<Goal> =
    goalService
      .create(
        CreateGoalRequestDto(
          userId = user.id,
          title = request.title,
          description = request.description,
          targetDate = request.targetDate,
        ),
      ).let { goal -> ServiceResponse.from(goal) }
}
