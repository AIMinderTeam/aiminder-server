package ai.aiminder.aiminderserver.schedule.repository

import ai.aiminder.aiminderserver.schedule.entity.ScheduleEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduleRepository : CoroutineCrudRepository<ScheduleEntity, UUID>
