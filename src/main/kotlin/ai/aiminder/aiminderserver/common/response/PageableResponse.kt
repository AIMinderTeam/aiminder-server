package ai.aiminder.aiminderserver.common.response

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

@Schema(description = "페이지네이션 응답 데이터")
data class PageableResponse(
  @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
  val page: Int,
  @Schema(description = "현재 페이지의 요소 개수", example = "10")
  val count: Int,
  @Schema(description = "전체 페이지 수", example = "5")
  val totalPages: Int,
  @Schema(description = "전체 요소 개수", example = "50")
  val totalElements: Long,
) {
  companion object {
    fun from(page: Page<*>): PageableResponse =
      PageableResponse(
        page = page.number,
        count = page.count(),
        totalPages = page.totalPages,
        totalElements = page.totalElements,
      )
  }
}
