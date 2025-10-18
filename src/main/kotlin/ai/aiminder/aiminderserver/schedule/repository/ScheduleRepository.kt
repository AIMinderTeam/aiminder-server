package ai.aiminder.aiminderserver.schedule.repository

import ai.aiminder.aiminderserver.schedule.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface ScheduleRepository : CoroutineCrudRepository<ScheduleEntity, UUID> {
  suspend fun findAllByGoalIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
    goalId: UUID,
    startDate: Instant,
    endDate: Instant,
  ): Flow<ScheduleEntity>
}
