package ai.aiminder.aiminderserver.schedule.dto

import ai.aiminder.aiminderserver.assistant.domain.ServiceToolContext
import ai.aiminder.aiminderserver.schedule.domain.ScheduleDraft
import java.time.Instant
import java.util.UUID

data class CreateScheduleRequest(
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
      schedule: ScheduleDraft,
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
