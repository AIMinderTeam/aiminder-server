package ai.aiminder.aiminderserver.image.dto

import ai.aiminder.aiminderserver.image.domain.Image
import io.swagger.v3.oas.annotations.media.Schema
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

@Schema(description = "이미지 업로드 응답 데이터")
data class ImageResponse(
  @Schema(description = "이미지 고유 ID", example = "123e4567-e89b-12d3-a456-426614174000")
  val id: UUID,
  @Schema(description = "사용자 ID", example = "2f6a3a4c-1c3b-4bde-9d2a-6c2c8b6a1e7f")
  val userId: UUID,
  @Schema(description = "원본 파일명", example = "profile-image.jpg")
  val originalFileName: String,
  @Schema(description = "저장된 파일명", example = "20240101_123456_profile-image.jpg")
  val storedFileName: String,
  @Schema(description = "파일 저장 경로", example = "/uploads/images/20240101_123456_profile-image.jpg")
  val filePath: String,
  @Schema(description = "파일 크기 (바이트)", example = "1024000")
  val fileSize: Long,
  @Schema(description = "파일 MIME 타입", example = "image/jpeg")
  val contentType: String,
  @Schema(description = "생성일시", example = "2024-01-01T00:00:00Z")
  val createdAt: Instant,
  @Schema(description = "수정일시", example = "2024-01-01T00:00:00Z")
  val updatedAt: Instant,
  @Schema(description = "삭제일시", example = "null")
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
