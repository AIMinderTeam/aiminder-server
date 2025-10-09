package ai.aiminder.aiminderserver.assistant.tool

import ai.aiminder.aiminderserver.assistant.domain.AiSchedule
import ai.aiminder.aiminderserver.assistant.domain.GoalDraft
import ai.aiminder.aiminderserver.assistant.domain.ServiceToolContext
import ai.aiminder.aiminderserver.assistant.dto.GoalMilestone
import ai.aiminder.aiminderserver.assistant.dto.UpdateConversationDto
import ai.aiminder.aiminderserver.assistant.repository.AiScheduleRepository
import ai.aiminder.aiminderserver.assistant.service.ConversationService
import ai.aiminder.aiminderserver.assistant.service.ToolContextService
import ai.aiminder.aiminderserver.common.util.logger
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequestDto
import ai.aiminder.aiminderserver.goal.dto.GoalResponse
import ai.aiminder.aiminderserver.goal.service.GoalService
import kotlinx.coroutines.runBlocking
import org.springframework.ai.chat.model.ToolContext
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class GoalTool(
  private val aiScheduleRepository: AiScheduleRepository,
  private val goalService: GoalService,
  private val conversationService: ConversationService,
  private val toolContextService: ToolContextService,
) : AssistantTool {
  private val logger = logger()

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
  ): GoalDraft {
    logger.debug(
      "도구 호출: refineGoal(목표제목={}, 목표날짜={}, 목표설명={}, 마일스톤={})",
      goalTitle,
      goalTargetDate,
      goalDescription,
      milestones,
    )
    return GoalDraft(
      goalTitle,
      goalTargetDate,
      goalDescription,
      milestones,
    )
  }

  @Tool(description = "사용자가 목표 저장을 요청할 경우 확정된 SMART 목표를 기반으로 데이터베이스에 저장한다")
  fun saveGoal(
    @ToolParam(required = true) draft: GoalDraft,
    toolContext: ToolContext,
  ): GoalResponse =
    runBlocking {
      logger.debug("도구 호출: saveGoal(초안={}, 도구컨텍스트={})", draft, toolContext)
      val serviceToolContext: ServiceToolContext = toolContextService.getContext(toolContext)
      val createdGoal: GoalResponse = goalService.create(CreateGoalRequestDto.from(draft, serviceToolContext))
      conversationService.update(UpdateConversationDto(serviceToolContext.conversationId, createdGoal.id))
      createdGoal
    }

  @Tool(description = "사용자가 일정 저장을 요청할 경우 제안된 일정을 데이터베이스에 저장한다")
  fun saveSchedules(
    @ToolParam(required = true) aiSchedules: List<AiSchedule>,
  ): List<AiSchedule> {
    logger.debug("도구 호출: saveSchedules(AI일정={})", aiSchedules)
    return aiScheduleRepository.save(aiSchedules)
  }
}
