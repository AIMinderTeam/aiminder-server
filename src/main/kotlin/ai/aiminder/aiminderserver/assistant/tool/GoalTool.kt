package ai.aiminder.aiminderserver.assistant.tool

import ai.aiminder.aiminderserver.assistant.domain.Goal
import ai.aiminder.aiminderserver.assistant.domain.GoalDraft
import ai.aiminder.aiminderserver.assistant.domain.Schedule
import ai.aiminder.aiminderserver.assistant.dto.GoalMilestone
import ai.aiminder.aiminderserver.assistant.repository.GoalRepository
import ai.aiminder.aiminderserver.assistant.repository.ScheduleRepository
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class GoalTool(
  private val goalRepository: GoalRepository,
  private val scheduleRepository: ScheduleRepository,
) : AssistantTool {
  @Tool(
    description = """
        주어진 목표를 SMART(Specific, Measurable, Achievable, Relevant, Time-bound) 기준에 맞춰
        구체화하고 부족한 정보가 있으면 사용자에게 질문한다
        """,
  )
  fun refineGoal(
    @ToolParam(required = true) goalTitle: String,
    @ToolParam(required = true) goalTargetDate: LocalDate,
    @ToolParam(required = true) goalDescription: String,
    @ToolParam(required = true) milestones: List<GoalMilestone>,
  ): GoalDraft =
    GoalDraft(
      goalTitle,
      goalTargetDate,
      goalDescription,
      milestones,
    )

  @Tool(description = "확정된 SMART 목표를 데이터베이스에 저장한다")
  fun saveGoal(
    @ToolParam(required = true) draft: GoalDraft,
  ): Goal = goalRepository.save(Goal.create(draft))

  @Tool(description = "제안된 일정을 데이터베이스에 저장한다")
  fun saveSchedules(
    @ToolParam(required = true) schedules: List<Schedule>,
  ): List<Schedule> = scheduleRepository.save(schedules)
}