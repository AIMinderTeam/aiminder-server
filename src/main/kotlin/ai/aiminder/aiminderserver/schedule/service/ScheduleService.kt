package ai.aiminder.aiminderserver.schedule.service

import ai.aiminder.aiminderserver.schedule.domain.Schedule
import ai.aiminder.aiminderserver.schedule.dto.CreateScheduleRequestDto
import ai.aiminder.aiminderserver.schedule.dto.GetSchedulesRequestDto
import ai.aiminder.aiminderserver.schedule.dto.ScheduleResponse
import ai.aiminder.aiminderserver.schedule.dto.UpdateScheduleRequestDto
import ai.aiminder.aiminderserver.schedule.entity.ScheduleEntity
import ai.aiminder.aiminderserver.schedule.error.ScheduleError
import ai.aiminder.aiminderserver.schedule.repository.ScheduleRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ScheduleService(
  private val scheduleRepository: ScheduleRepository,
) {
  suspend fun create(dto: CreateScheduleRequestDto): ScheduleResponse {
    validateDateRange(dto.startDate, dto.endDate)

    val schedule =
      ScheduleEntity
        .from(dto)
        .let { scheduleRepository.save(it) }
        .let { Schedule.from(it) }

    return ScheduleResponse.from(schedule)
  }

  suspend fun get(dto: GetSchedulesRequestDto): Page<ScheduleResponse> {
    val schedules =
      when {
        dto.goalId != null -> {
          scheduleRepository
            .findByUserIdAndGoalIdAndDeletedAtIsNull(
              userId = dto.userId,
              goalId = dto.goalId,
              pageable = dto.pageable,
            )
        }

        dto.status != null -> {
          scheduleRepository
            .findByUserIdAndStatusAndDeletedAtIsNull(
              userId = dto.userId,
              status = dto.status,
              pageable = dto.pageable,
            )
        }

        dto.startDate != null && dto.endDate != null -> {
          scheduleRepository
            .findByUserIdAndStartDateBetweenAndDeletedAtIsNull(
              userId = dto.userId,
              startDate = dto.startDate,
              endDate = dto.endDate,
              pageable = dto.pageable,
            )
        }

        else -> {
          scheduleRepository
            .findByUserIdAndDeletedAtIsNull(
              userId = dto.userId,
              pageable = dto.pageable,
            )
        }
      }.map { Schedule.from(it) }
        .map { ScheduleResponse.from(it) }
        .toList()

    val totalCount = scheduleRepository.countByUserIdAndDeletedAtIsNull(dto.userId)

    return PageImpl(schedules, dto.pageable, totalCount)
  }

  suspend fun update(dto: UpdateScheduleRequestDto): ScheduleResponse {
    val existingSchedule = scheduleRepository.findById(dto.id) ?: throw ScheduleError.NotFound()

    if (existingSchedule.userId != dto.userId || existingSchedule.deletedAt != null) {
      throw ScheduleError.AccessDenied()
    }

    if (dto.startDate != null && dto.endDate != null) {
      validateDateRange(dto.startDate, dto.endDate)
    }

    val updatedSchedule =
      existingSchedule.copy(
        title = dto.title ?: existingSchedule.title,
        description = dto.description ?: existingSchedule.description,
        status = dto.status ?: existingSchedule.status,
        startDate = dto.startDate ?: existingSchedule.startDate,
        endDate = dto.endDate ?: existingSchedule.endDate,
        updatedAt = Instant.now(),
      )

    val savedSchedule =
      scheduleRepository
        .save(updatedSchedule)
        .let { Schedule.from(it) }

    return ScheduleResponse.from(savedSchedule)
  }

  suspend fun delete(
    scheduleId: UUID,
    userId: UUID,
  ) {
    val existingSchedule = scheduleRepository.findById(scheduleId) ?: throw ScheduleError.NotFound()

    if (existingSchedule.userId != userId || existingSchedule.deletedAt != null) {
      throw ScheduleError.AccessDenied()
    }
    val now = Instant.now()

    val deletedSchedule =
      existingSchedule.copy(
        deletedAt = now,
        updatedAt = now,
      )

    scheduleRepository.save(deletedSchedule)
  }

  suspend fun findById(
    scheduleId: UUID,
    userId: UUID,
  ): ScheduleResponse {
    val schedule = scheduleRepository.findById(scheduleId) ?: throw ScheduleError.NotFound()

    if (schedule.userId != userId || schedule.deletedAt != null) {
      throw ScheduleError.AccessDenied()
    }

    return ScheduleResponse.from(Schedule.from(schedule))
  }

  private fun validateDateRange(
    startDate: Instant,
    endDate: Instant,
  ) {
    if (startDate.isAfter(endDate)) {
      throw ScheduleError.InvalidDateRange()
    }
  }
}
