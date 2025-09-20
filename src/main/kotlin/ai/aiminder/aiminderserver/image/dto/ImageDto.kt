package ai.aiminder.aiminderserver.image.dto

import ai.aiminder.aiminderserver.image.domain.Image
import java.time.Instant
import java.util.UUID

data class UploadImageRequestDto(
  val userId: UUID,
  val originalFileName: String,
  val storedFileName: String,
  val filePath: String,
  val fileSize: Long,
  val contentType: String,
)

data class ImageResponse(
  val id: UUID,
  val userId: UUID,
  val originalFileName: String,
  val storedFileName: String,
  val filePath: String,
  val fileSize: Long,
  val contentType: String,
  val createdAt: Instant,
  val updatedAt: Instant,
  val deletedAt: Instant?,
) {
  companion object {
    fun from(image: Image): ImageResponse =
      ImageResponse(
        id = image.id,
        userId = image.userId,
        originalFileName = image.originalFileName,
        storedFileName = image.storedFileName,
        filePath = image.filePath,
        fileSize = image.fileSize,
        contentType = image.contentType,
        createdAt = image.createdAt,
        updatedAt = image.updatedAt,
        deletedAt = image.deletedAt,
      )
  }
}
