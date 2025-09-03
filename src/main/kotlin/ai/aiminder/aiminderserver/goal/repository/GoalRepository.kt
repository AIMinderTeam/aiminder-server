package ai.aiminder.aiminderserver.goal.repository

import ai.aiminder.aiminderserver.goal.entity.GoalEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GoalRepository : CoroutineCrudRepository<GoalEntity, UUID>
