package ai.aiminder.aiminderserver.image.domain

import ai.aiminder.aiminderserver.image.entity.ImageEntity
import java.time.Instant
import java.util.UUID

data class Image(
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
    fun from(imageEntity: ImageEntity): Image =
      Image(
        id = imageEntity.id!!,
        userId = imageEntity.userId,
        originalFileName = imageEntity.originalFileName,
        storedFileName = imageEntity.storedFileName,
        filePath = imageEntity.filePath,
        fileSize = imageEntity.fileSize,
        contentType = imageEntity.contentType,
        createdAt = imageEntity.createdAt,
        updatedAt = imageEntity.updatedAt,
        deletedAt = imageEntity.deletedAt,
      )
  }
}
