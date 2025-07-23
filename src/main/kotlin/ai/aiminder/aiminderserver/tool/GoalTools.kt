package ai.aiminder.aiminderserver.tool

import ai.aiminder.aiminderserver.domain.Goal
import ai.aiminder.aiminderserver.domain.GoalDraft
import ai.aiminder.aiminderserver.repository.GoalRepository
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class GoalTools(
    private val goalRepository: GoalRepository,
) {
    @Tool(
        description = """
        주어진 목표를 SMART(Specific, Measurable, Achievable, Relevant, Time-bound) 기준에 맞춰
        구체화하고 부족한 정보가 있으면 사용자에게 질문한다
        """,
    )
    fun refineGoalToSmart(
        @ToolParam(required = true) originalText: String,
        @ToolParam(required = true) metric: String,
        @ToolParam(required = true) targetDate: LocalDateTime,
    ): GoalDraft {
        val smartText =
            """
            목표: %s
            측정 지표: %s
            기한: %s
            """.trimIndent().format(originalText, metric, targetDate)

        return GoalDraft(
            originalText = originalText,
            smartText = smartText,
            metric = metric,
            targetDate = targetDate,
        )
    }

    @Tool(description = "확정된 SMART 목표를 데이터베이스에 저장한다")
    fun saveGoal(draft: GoalDraft): Goal = goalRepository.save(Goal.create(draft))
}
