package ai.aiminder.aiminderserver.assistant.tool

import ai.aiminder.aiminderserver.common.util.logger
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class TodayTool {
  private val logger = logger()

  @Tool(description = "오늘 날짜를 조회한다")
  fun getToday(): LocalDate {
    logger.debug("도구 호출: getToday()")
    return LocalDate.now()
  }
}
