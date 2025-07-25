package ai.aiminder.aiminderserver.tool

import ai.aiminder.aiminderserver.domain.Goal
import ai.aiminder.aiminderserver.domain.GoalDraft
import ai.aiminder.aiminderserver.dto.GoalMilestone
import ai.aiminder.aiminderserver.repository.GoalRepository
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class GoalTool(
    private val goalRepository: GoalRepository,
) : AssistantTool {
    @Tool(
        description = """
        주어진 목표를 SMART(Specific, Measurable, Achievable, Relevant, Time-bound) 기준에 맞춰
        구체화하고 부족한 정보가 있으면 사용자에게 질문한다
        """,
    )
    fun refineGoal(
        @ToolParam(required = true) goalTitle: String,
        @ToolParam(required = true) goalTargetDate: Instant,
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
}
