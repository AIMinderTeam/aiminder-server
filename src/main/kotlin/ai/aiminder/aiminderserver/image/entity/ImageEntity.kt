package ai.aiminder.aiminderserver.image.entity

import ai.aiminder.aiminderserver.image.dto.UploadImageRequestDto
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("images")
data class ImageEntity(
  @Id
  @Column("image_id")
  @get:JvmName("imageId")
  val id: UUID? = null,
  val userId: UUID,
  val originalFileName: String,
  val storedFileName: String,
  val filePath: String,
  val fileSize: Long,
  val contentType: String,
  val createdAt: Instant = Instant.now(),
  val updatedAt: Instant = createdAt,
  val deletedAt: Instant? = null,
) : Persistable<UUID> {
  override fun getId(): UUID? = id

  override fun isNew(): Boolean = id == null

  companion object {
    fun from(dto: UploadImageRequestDto): ImageEntity =
      ImageEntity(
        userId = dto.userId,
        originalFileName = dto.originalFileName,
        storedFileName = dto.storedFileName,
        filePath = dto.filePath,
        fileSize = dto.fileSize,
        contentType = dto.contentType,
      )
  }
}
