package ai.aiminder.aiminderserver.common.response

import org.springframework.data.domain.Page

data class PageableResponse(
  val page: Int,
  val count: Int,
  val totalPages: Int,
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
