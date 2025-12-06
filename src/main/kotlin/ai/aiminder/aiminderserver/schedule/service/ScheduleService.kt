package ai.aiminder.aiminderserver.schedule.service

import ai.aiminder.aiminderserver.schedule.domain.Schedule
import ai.aiminder.aiminderserver.schedule.dto.CreateScheduleRequestDto
import ai.aiminder.aiminderserver.schedule.dto.GetSchedulesRequestDto
import ai.aiminder.aiminderserver.schedule.dto.GoalScheduleStatistics
import ai.aiminder.aiminderserver.schedule.dto.MonthlyScheduleStatisticsResponse
import ai.aiminder.aiminderserver.schedule.dto.ScheduleResponse
import ai.aiminder.aiminderserver.schedule.dto.UpdateScheduleRequestDto
import ai.aiminder.aiminderserver.schedule.entity.ScheduleEntity
import ai.aiminder.aiminderserver.schedule.error.ScheduleError
import ai.aiminder.aiminderserver.schedule.repository.ScheduleQueryRepository
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
  private val scheduleQueryRepository: ScheduleQueryRepository,
) {
  suspend fun create(dto: CreateScheduleRequestDto): ScheduleResponse {
    val schedule =
      ScheduleEntity
        .from(dto)
        .let { scheduleRepository.save(it) }
        .let { Schedule.fromEntity(it) }

    return ScheduleResponse.from(schedule)
  }

  suspend fun create(dto: List<CreateScheduleRequestDto>): List<Schedule> =
    dto
      .map { ScheduleEntity.from(it) }
      .let { scheduleRepository.saveAll(it) }
      .map { Schedule.fromEntity(it) }
      .toList()

  suspend fun get(dto: GetSchedulesRequestDto): Page<ScheduleResponse> {
    val schedules =
      scheduleQueryRepository
        .findSchedulesBy(dto)
        .map { ScheduleResponse.from(Schedule.fromRow(it)) }
        .toList()

    val totalCount = scheduleQueryRepository.countBy(dto)

    return PageImpl(schedules, dto.pageable, totalCount)
  }

  suspend fun get(
    goalId: UUID,
    startDate: Instant,
    endDate: Instant,
  ): List<Schedule> =
    scheduleRepository
      .findAllByGoalIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(goalId, startDate, endDate)
      .map { Schedule.fromEntity(it) }
      .toList()

  suspend fun update(dto: UpdateScheduleRequestDto): ScheduleResponse {
    val existingSchedule = scheduleRepository.findById(dto.id) ?: throw ScheduleError.NotFound()

    if (existingSchedule.userId != dto.userId || existingSchedule.deletedAt != null) {
      throw ScheduleError.AccessDenied()
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
        .let { Schedule.fromEntity(it) }

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

    return ScheduleResponse.from(Schedule.fromEntity(schedule))
  }

  suspend fun getMonthlyStatistics(
    userId: UUID,
    year: Int,
    month: Int,
  ): MonthlyScheduleStatisticsResponse {
    val dailyStatistics = scheduleQueryRepository.findDailyStatisticsForMonth(userId, year, month)

    return MonthlyScheduleStatisticsResponse.from(
      year = year,
      month = month,
      dailyStatistics = dailyStatistics,
    )
  }

  suspend fun getScheduleStatisticsByGoalIds(goalIds: List<UUID>): Map<UUID, GoalScheduleStatistics> =
    scheduleQueryRepository
      .findScheduleStatisticsByGoalIds(goalIds)
      .associateBy { it.goalId }
}
