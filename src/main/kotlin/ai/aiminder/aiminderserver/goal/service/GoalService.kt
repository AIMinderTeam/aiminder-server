package ai.aiminder.aiminderserver.goal.service

import ai.aiminder.aiminderserver.common.util.logger
import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequestDto
import ai.aiminder.aiminderserver.goal.dto.GetGoalsRequestDto
import ai.aiminder.aiminderserver.goal.dto.GoalResponse
import ai.aiminder.aiminderserver.goal.entity.GoalEntity
import ai.aiminder.aiminderserver.goal.error.GoalError
import ai.aiminder.aiminderserver.goal.repository.GoalRepository
import ai.aiminder.aiminderserver.image.repository.ImageRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GoalService(
  private val goalRepository: GoalRepository,
  private val imageRepository: ImageRepository,
) {
  private val logger = logger()

  suspend fun create(dto: CreateGoalRequestDto): GoalResponse {
    val goal =
      GoalEntity
        .from(dto)
        .let { goalRepository.save(it) }
        .let { Goal.from(it) }

    val imagePath = getImagePath(goal.imageId)
    return GoalResponse.from(goal, imagePath)
  }

  suspend fun get(dto: GetGoalsRequestDto): Page<GoalResponse> {
    val goals =
      goalRepository
        .findByStatusAndDeletedAtIsNullAndUserId(
          status = dto.status,
          userId = dto.userId,
          pageable = dto.pageable,
        ).map { Goal.from(it) }
        .toList()

    val goalResponses =
      goals.map { goal ->
        val imagePath = getImagePath(goal.imageId)
        GoalResponse.from(goal, imagePath)
      }

    val totalCount =
      goalRepository.countByStatusIsAndDeletedAtIsNullAndUserIdIs(
        status = dto.status,
        userId = dto.userId,
      )

    return PageImpl(goalResponses, dto.pageable, totalCount)
  }

  suspend fun get(goalId: UUID): Goal =
    goalRepository
      .findById(goalId)
      ?.let { Goal.from(it) }
      ?: throw GoalError.GoalNotFound(goalId)

  private suspend fun getImagePath(imageId: UUID?): String? =
    imageId?.let { id ->
      try {
        imageRepository.findByIdAndDeletedAtIsNull(id)?.filePath
      } catch (e: Exception) {
        logger.warn("Failed to fetch image for id: $id", e)
        null
      }
    }
}
