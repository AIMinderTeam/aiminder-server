package ai.aiminder.aiminderserver.schedule.controller

import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.schedule.dto.CreateScheduleRequest
import ai.aiminder.aiminderserver.schedule.dto.CreateScheduleRequestDto
import ai.aiminder.aiminderserver.schedule.dto.GetSchedulesRequest
import ai.aiminder.aiminderserver.schedule.dto.GetSchedulesRequestDto
import ai.aiminder.aiminderserver.schedule.dto.ScheduleResponse
import ai.aiminder.aiminderserver.schedule.dto.UpdateScheduleRequest
import ai.aiminder.aiminderserver.schedule.dto.UpdateScheduleRequestDto
import ai.aiminder.aiminderserver.schedule.service.ScheduleService
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
class ScheduleController(
  private val scheduleService: ScheduleService,
) : ScheduleControllerDocs {
  @PostMapping("/{goalId}/schedules")
  override suspend fun createSchedule(
    @PathVariable
    goalId: UUID,
    @RequestBody
    request: CreateScheduleRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<ScheduleResponse> =
    scheduleService
      .create(
        CreateScheduleRequestDto(
          goalId = goalId,
          userId = user.id,
          title = request.title,
          description = request.description,
          startDate = request.startDate,
          endDate = request.endDate,
        ),
      ).let { schedule -> ServiceResponse.from(schedule) }

  @GetMapping("/{goalId}/schedules")
  override suspend fun getSchedules(
    @PathVariable
    goalId: UUID,
    request: GetSchedulesRequest,
    pageable: PageableRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<List<ScheduleResponse>> =
    scheduleService
      .get(
        GetSchedulesRequestDto.from(
          goalId = goalId,
          getSchedulesRequest = request,
          user = user,
          pageable = pageable,
        ),
      ).let { schedules -> ServiceResponse.from(schedules) }

  @PutMapping("/schedules/{scheduleId}")
  override suspend fun updateSchedule(
    @PathVariable
    scheduleId: UUID,
    @RequestBody
    request: UpdateScheduleRequest,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<ScheduleResponse> {
    val updatedSchedule =
      scheduleService.update(
        UpdateScheduleRequestDto(
          id = scheduleId,
          userId = user.id,
          title = request.title,
          description = request.description,
          status = request.status,
          startDate = request.startDate,
          endDate = request.endDate,
        ),
      )

    return ServiceResponse.from(updatedSchedule)
  }

  @DeleteMapping("/schedules/{scheduleId}")
  override suspend fun deleteSchedule(
    @PathVariable
    scheduleId: UUID,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<String> {
    scheduleService.delete(scheduleId, user.id)
    return ServiceResponse.from("Schedule deleted successfully")
  }

  @GetMapping("/schedules/{scheduleId}")
  override suspend fun getScheduleById(
    @PathVariable
    scheduleId: UUID,
    @AuthenticationPrincipal
    user: User,
  ): ServiceResponse<ScheduleResponse> {
    val schedule = scheduleService.findById(scheduleId, user.id)
    return ServiceResponse.from(schedule)
  }
}
