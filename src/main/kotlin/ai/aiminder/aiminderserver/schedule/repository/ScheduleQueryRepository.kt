package ai.aiminder.aiminderserver.schedule.repository

import ai.aiminder.aiminderserver.common.config.JooqR2dbcRepository
import ai.aiminder.aiminderserver.jooq.tables.Schedules.Companion.SCHEDULES
import ai.aiminder.aiminderserver.schedule.dto.GetSchedulesRequestDto
import ai.aiminder.aiminderserver.schedule.repository.row.ScheduleRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import org.jooq.Condition
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class ScheduleQueryRepository : JooqR2dbcRepository() {
  suspend fun findSchedulesBy(dto: GetSchedulesRequestDto): Flow<ScheduleRow> =
    query {
      select(
        SCHEDULES.SCHEDULE_ID,
        SCHEDULES.GOAL_ID,
        SCHEDULES.USER_ID,
        SCHEDULES.TITLE,
        SCHEDULES.DESCRIPTION,
        SCHEDULES.STATUS,
        SCHEDULES.START_DATE,
        SCHEDULES.END_DATE,
        SCHEDULES.CREATED_AT,
        SCHEDULES.UPDATED_AT,
        SCHEDULES.DELETED_AT,
      )
        .from(SCHEDULES)
        .where(buildScheduleConditions(dto))
        .orderBy(SCHEDULES.CREATED_AT.desc())
        .offset(dto.pageable.offset.toInt())
        .limit(dto.pageable.pageSize)
    }.map { record ->
      ScheduleRow(
        scheduleId = record.get(SCHEDULES.SCHEDULE_ID)!!,
        goalId = record.get(SCHEDULES.GOAL_ID)!!,
        userId = record.get(SCHEDULES.USER_ID)!!,
        title = record.get(SCHEDULES.TITLE)!!,
        description = record.get(SCHEDULES.DESCRIPTION),
        status = record.get(SCHEDULES.STATUS)!!,
        startDate = record.get(SCHEDULES.START_DATE)!!,
        endDate = record.get(SCHEDULES.END_DATE)!!,
        createdAt = record.get(SCHEDULES.CREATED_AT)!!,
        updatedAt = record.get(SCHEDULES.UPDATED_AT)!!,
        deletedAt = record.get(SCHEDULES.DELETED_AT),
      )
    }

  suspend fun countBy(dto: GetSchedulesRequestDto): Long =
    query {
      selectCount()
        .from(SCHEDULES)
        .where(buildScheduleConditions(dto))
    }.single().component1().toLong()

  private fun buildScheduleConditions(dto: GetSchedulesRequestDto): List<Condition> {
    val conditions = mutableListOf<Condition>()

    // Base conditions
    conditions.add(SCHEDULES.DELETED_AT.isNull)
    conditions.add(SCHEDULES.USER_ID.eq(dto.userId))

    // Dynamic conditions
    dto.goalId?.let { conditions.add(SCHEDULES.GOAL_ID.eq(it)) }
    dto.status?.let { conditions.add(SCHEDULES.STATUS.eq(it.name)) }

    if (dto.startDate != null && dto.endDate != null) {
      val startDateTime = dto.startDate.toLocalDateTime()
      val endDateTime = dto.endDate.toLocalDateTime()
      conditions.add(SCHEDULES.START_DATE.between(startDateTime, endDateTime))
    }

    return conditions
  }

  private fun Instant.toLocalDateTime(): LocalDateTime = atOffset(ZoneOffset.UTC).toLocalDateTime()
}
