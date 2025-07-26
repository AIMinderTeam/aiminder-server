package ai.aiminder.aiminderserver.assistant.repository

import ai.aiminder.aiminderserver.assistant.domain.Schedule
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class ScheduleRepository {
    private val database = mutableSetOf<Schedule>()
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun save(schedules: List<Schedule>): List<Schedule> {
        logger.info("Saving schedules: $schedules")
        database.addAll(schedules)
        return schedules
    }
}
