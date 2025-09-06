package ai.aiminder.aiminderserver.common.request

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

data class PageableRequest(
  val page: Int = 0,
  val size: Int = 10,
  val sort: String = "createdAt",
  val direction: Sort.Direction = Sort.Direction.DESC,
) {
  fun toDomain(): Pageable =
    PageRequest.of(
      this.page,
      this.size,
      Sort.by(this.direction, this.sort),
    )
}
