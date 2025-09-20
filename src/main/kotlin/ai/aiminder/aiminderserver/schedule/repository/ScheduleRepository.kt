package ai.aiminder.aiminderserver.schedule.repository

import ai.aiminder.aiminderserver.schedule.domain.ScheduleStatus
import ai.aiminder.aiminderserver.schedule.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface ScheduleRepository : CoroutineCrudRepository<ScheduleEntity, UUID> {
  suspend fun findByUserIdAndDeletedAtIsNull(
    userId: UUID,
    pageable: Pageable,
  ): Flow<ScheduleEntity>

  suspend fun findByGoalIdAndDeletedAtIsNull(
    goalId: UUID,
    pageable: Pageable,
  ): Flow<ScheduleEntity>

  suspend fun findByUserIdAndGoalIdAndDeletedAtIsNull(
    userId: UUID,
    goalId: UUID,
    pageable: Pageable,
  ): Flow<ScheduleEntity>

  suspend fun findByUserIdAndStatusAndDeletedAtIsNull(
    userId: UUID,
    status: ScheduleStatus,
    pageable: Pageable,
  ): Flow<ScheduleEntity>

  suspend fun findByUserIdAndStartDateBetweenAndDeletedAtIsNull(
    userId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    pageable: Pageable,
  ): Flow<ScheduleEntity>

  suspend fun countByUserIdAndDeletedAtIsNull(userId: UUID): Long

  suspend fun countByGoalIdAndDeletedAtIsNull(goalId: UUID): Long

  suspend fun findByIdAndUserIdAndDeletedAtIsNull(
    scheduleId: UUID,
    userId: UUID,
  ): ScheduleEntity?
}
