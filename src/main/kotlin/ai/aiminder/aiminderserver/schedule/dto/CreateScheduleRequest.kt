package ai.aiminder.aiminderserver.schedule.dto

import ai.aiminder.aiminderserver.assistant.domain.AiSchedule
import ai.aiminder.aiminderserver.assistant.domain.ServiceToolContext
import java.time.Instant
import java.util.UUID

data class CreateScheduleRequest(
  val goalId: UUID,
  val title: String,
  val description: String? = null,
  val startDate: Instant,
  val endDate: Instant,
)

data class CreateScheduleRequestDto(
  val goalId: UUID,
  val userId: UUID,
  val title: String,
  val description: String? = null,
  val startDate: Instant,
  val endDate: Instant,
) {
  companion object {
    fun from(
      schedule: AiSchedule,
      serviceToolContext: ServiceToolContext,
    ): CreateScheduleRequestDto =
      CreateScheduleRequestDto(
        goalId = serviceToolContext.goalId!!,
        userId = serviceToolContext.userId,
        title = schedule.task,
        startDate = schedule.date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
        endDate = schedule.date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
      )
  }
}
