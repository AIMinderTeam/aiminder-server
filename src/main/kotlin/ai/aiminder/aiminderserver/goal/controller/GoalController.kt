package ai.aiminder.aiminderserver.goal.controller

import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequest
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequestDto
import ai.aiminder.aiminderserver.goal.dto.DeleteGoalRequestDto
import ai.aiminder.aiminderserver.goal.dto.GetGoalsRequest
import ai.aiminder.aiminderserver.goal.dto.GetGoalsRequestDto
import ai.aiminder.aiminderserver.goal.dto.GoalResponse
import ai.aiminder.aiminderserver.goal.dto.UpdateGoalRequest
import ai.aiminder.aiminderserver.goal.dto.UpdateGoalRequestDto
import ai.aiminder.aiminderserver.goal.service.GoalService
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

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
  ): ServiceResponse<GoalResponse> =
    goalService
      .create(
        CreateGoalRequestDto(
          userId = user.id,
          title = request.title,
          description = request.description,
          targetDate = request.targetDate,
          imageId = request.imageId,
        ),
      ).let { goal -> ServiceResponse.from(goal) }

  @GetMapping
  override suspend fun getGoals(
    request: GetGoalsRequest,
    pageable: PageableRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<List<GoalResponse>> =
    goalService
      .get(
        GetGoalsRequestDto.from(
          getGoalsRequest = request,
          user = user,
          pageable = pageable,
        ),
      ).let { goals -> ServiceResponse.from(goals) }

  @PutMapping("/{goalId}")
  suspend fun updateGoal(
    @PathVariable
    goalId: UUID,
    @RequestBody
    request: UpdateGoalRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<GoalResponse> {
    val updatedGoal: GoalResponse = goalService.update(UpdateGoalRequestDto.from(goalId, request, user))
    return ServiceResponse.from(updatedGoal)
  }

  @DeleteMapping("/{goalId}")
  suspend fun deleteGoal(
    @PathVariable
    goalId: UUID,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<String> {
    val dto = DeleteGoalRequestDto(goalId, user.id)
    goalService.delete(dto)
    return ServiceResponse.from("Goal deleted successfully")
  }
}
