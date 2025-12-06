package ai.aiminder.aiminderserver.schedule.repository

import ai.aiminder.aiminderserver.common.config.JooqR2dbcRepository
import ai.aiminder.aiminderserver.jooq.tables.Goals.Companion.GOALS
import ai.aiminder.aiminderserver.jooq.tables.Schedules.Companion.SCHEDULES
import ai.aiminder.aiminderserver.schedule.dto.DailyScheduleStatistics
import ai.aiminder.aiminderserver.schedule.dto.GetSchedulesRequestDto
import ai.aiminder.aiminderserver.schedule.dto.GoalScheduleStatistics
import ai.aiminder.aiminderserver.schedule.repository.row.ScheduleRow
import ai.aiminder.aiminderserver.schedule.repository.row.ScheduleWithGoalRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import org.jooq.Condition
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

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
      ).from(SCHEDULES)
        .where(buildScheduleConditions(dto))
        .orderBy(SCHEDULES.START_DATE.asc())
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

  suspend fun findDailyStatisticsForMonth(
    userId: UUID,
    year: Int,
    month: Int,
  ): List<DailyScheduleStatistics> =
    query {
      val dayField = DSL.extract(SCHEDULES.START_DATE, org.jooq.DatePart.DAY)
      val totalCountField = DSL.count()
      val completedCountField =
        DSL.count(
          DSL.`when`(SCHEDULES.STATUS.eq("COMPLETED"), 1).otherwise(null as Int?),
        )

      select(
        dayField.`as`("date"),
        totalCountField.`as`("total_count"),
        completedCountField.`as`("completed_count"),
      ).from(SCHEDULES)
        .where(
          SCHEDULES.USER_ID.eq(userId)
            .and(DSL.extract(SCHEDULES.START_DATE, org.jooq.DatePart.YEAR).eq(year))
            .and(DSL.extract(SCHEDULES.START_DATE, org.jooq.DatePart.MONTH).eq(month))
            .and(SCHEDULES.DELETED_AT.isNull),
        )
        .groupBy(dayField)
        .orderBy(dayField)
    }.map { record ->
      DailyScheduleStatistics.from(
        date = record.get("date") as Int,
        totalCount = record.get("total_count") as Int,
        completedCount = record.get("completed_count") as Int,
      )
    }.toList()

  private fun buildScheduleConditions(dto: GetSchedulesRequestDto): List<Condition> {
    val conditions = mutableListOf<Condition>()

    conditions.add(SCHEDULES.DELETED_AT.isNull)
    conditions.add(SCHEDULES.USER_ID.eq(dto.userId))

    dto.goalId.let { conditions.add(SCHEDULES.GOAL_ID.eq(it)) }
    dto.status?.let { conditions.add(SCHEDULES.STATUS.eq(it.name)) }

    if (dto.startDate != null && dto.endDate != null) {
      val startDateTime = dto.startDate.toLocalDateTime()
      val endDateTime = dto.endDate.toLocalDateTime()
      conditions.add(SCHEDULES.START_DATE.between(startDateTime, endDateTime))
    }

    return conditions
  }

  private fun Instant.toLocalDateTime(): LocalDateTime = atOffset(ZoneOffset.UTC).toLocalDateTime()

  suspend fun findScheduleStatisticsByGoalIds(goalIds: List<UUID>): List<GoalScheduleStatistics> {
    if (goalIds.isEmpty()) return emptyList()

    return query {
      val totalCountField = DSL.count()
      val completedCountField =
        DSL.count(
          DSL.`when`(SCHEDULES.STATUS.eq("COMPLETED"), 1).otherwise(null as Int?),
        )

      select(
        SCHEDULES.GOAL_ID,
        totalCountField.`as`("total_count"),
        completedCountField.`as`("completed_count"),
      ).from(SCHEDULES)
        .where(
          SCHEDULES.GOAL_ID.`in`(goalIds)
            .and(SCHEDULES.DELETED_AT.isNull),
        )
        .groupBy(SCHEDULES.GOAL_ID)
    }.map { record ->
      GoalScheduleStatistics(
        goalId = record.get(SCHEDULES.GOAL_ID)!!,
        totalCount = record.get("total_count") as Int,
        completedCount = record.get("completed_count") as Int,
      )
    }.toList()
  }

  suspend fun findSchedulesByDate(
    userId: UUID,
    date: LocalDate,
  ): List<ScheduleWithGoalRow> {
    val startOfDay = date.atStartOfDay()
    val endOfDay = date.plusDays(1).atStartOfDay()

    return query {
      select(
        SCHEDULES.SCHEDULE_ID,
        SCHEDULES.TITLE.`as`("schedule_title"),
        SCHEDULES.DESCRIPTION.`as`("schedule_description"),
        SCHEDULES.START_DATE,
        SCHEDULES.END_DATE,
        SCHEDULES.STATUS.`as`("schedule_status"),
        GOALS.GOAL_ID,
        GOALS.TITLE.`as`("goal_title"),
        GOALS.DESCRIPTION.`as`("goal_description"),
        GOALS.TARGET_DATE,
        GOALS.STATUS.`as`("goal_status"),
        GOALS.IMAGE_ID,
      ).from(SCHEDULES)
        .join(GOALS).on(SCHEDULES.GOAL_ID.eq(GOALS.GOAL_ID))
        .where(
          SCHEDULES.USER_ID.eq(userId)
            .and(SCHEDULES.DELETED_AT.isNull)
            .and(GOALS.DELETED_AT.isNull)
            .and(SCHEDULES.START_DATE.lt(endOfDay))
            .and(SCHEDULES.END_DATE.ge(startOfDay)),
        )
        .orderBy(SCHEDULES.START_DATE.asc())
    }.map { record ->
      ScheduleWithGoalRow(
        scheduleId = record.get(SCHEDULES.SCHEDULE_ID)!!,
        scheduleTitle = record.get("schedule_title") as String,
        scheduleDescription = record.get("schedule_description") as String?,
        startDate = record.get(SCHEDULES.START_DATE)!!,
        endDate = record.get(SCHEDULES.END_DATE)!!,
        scheduleStatus = record.get("schedule_status") as String,
        goalId = record.get(GOALS.GOAL_ID)!!,
        goalTitle = record.get("goal_title") as String,
        goalDescription = record.get("goal_description") as String?,
        targetDate = record.get(GOALS.TARGET_DATE)!!,
        goalStatus = record.get("goal_status") as String,
        imageId = record.get(GOALS.IMAGE_ID),
      )
    }.toList()
  }
}
