package ai.aiminder.aiminderserver.goal.controller

import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequest
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequestDto
import ai.aiminder.aiminderserver.goal.dto.GetGoalsRequest
import ai.aiminder.aiminderserver.goal.dto.GetGoalsRequestDto
import ai.aiminder.aiminderserver.goal.service.GoalService
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/goals")
class GoalController(
  private val goalService: GoalService,
) : GoalControllerDocs {
  @PostMapping
  override suspend fun createGoal(
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

  @GetMapping
  suspend fun getGoals(
    request: GetGoalsRequest,
    pageable: PageableRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<List<Goal>> =
    goalService
      .get(
        GetGoalsRequestDto.from(
          getGoalsRequest = request,
          user = user,
          pageable = pageable,
        ),
      ).let { goals -> ServiceResponse.from(goals) }
}
