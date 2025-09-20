package ai.aiminder.aiminderserver.schedule.entity

import ai.aiminder.aiminderserver.schedule.domain.ScheduleStatus
import ai.aiminder.aiminderserver.schedule.dto.CreateScheduleRequestDto
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("schedules")
data class ScheduleEntity(
  @Id
  @Column("schedule_id")
  @get:JvmName("scheduleId")
  val id: UUID? = null,
  val goalId: UUID,
  val userId: UUID,
  val title: String,
  val startDate: Instant,
  val endDate: Instant,
  val description: String? = null,
  val status: ScheduleStatus = ScheduleStatus.READY,
  val createdAt: Instant = Instant.now(),
  val updatedAt: Instant = createdAt,
  val deletedAt: Instant? = null,
) : Persistable<UUID> {
  override fun getId(): UUID? = id

  override fun isNew(): Boolean = id == null

  companion object {
    fun from(dto: CreateScheduleRequestDto): ScheduleEntity =
      ScheduleEntity(
        goalId = dto.goalId,
        userId = dto.userId,
        title = dto.title,
        description = dto.description,
        startDate = dto.startDate,
        endDate = dto.endDate,
      )
  }
}
