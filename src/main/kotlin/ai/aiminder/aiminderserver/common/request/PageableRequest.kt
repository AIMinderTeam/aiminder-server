package ai.aiminder.aiminderserver.common.request

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

data class PageableRequest(
  @field:Schema(description = "Page number", example = "0")
  val page: Int = 0,
  @field:Schema(description = "Size of page", example = "10")
  val size: Int = 10,
  @field:Schema(description = "Sorting field", example = "createdAt")
  val sort: String = "createdAt",
  @field:Schema(description = "Sorting direction", example = "DESC")
  val direction: Sort.Direction = Sort.Direction.DESC,
) {
  fun toDomain(): Pageable =
    PageRequest.of(
      this.page,
      this.size,
      Sort.by(this.direction, this.sort),
    )
}
