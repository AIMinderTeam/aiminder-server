package ai.aiminder.aiminderserver.image.service

import ai.aiminder.aiminderserver.image.domain.Image
import ai.aiminder.aiminderserver.image.dto.UploadImageRequestDto
import ai.aiminder.aiminderserver.image.entity.ImageEntity
import ai.aiminder.aiminderserver.image.error.ImageError
import ai.aiminder.aiminderserver.image.property.ImageProperties
import ai.aiminder.aiminderserver.image.repository.ImageRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.io.File
import java.util.UUID

@Service
class ImageService(
  private val repository: ImageRepository,
  private val imageProperties: ImageProperties,
) {
  init {
    imageProperties.createUploadDirectory()
  }

  suspend fun uploadImage(
    filePart: FilePart,
    userId: UUID,
  ): Image {
    validateFileType(filePart)

    val originalFileName = filePart.filename()
    val fileExtension = getFileExtension(originalFileName)
    val storedFileName = "${UUID.randomUUID()}$fileExtension"
    val filePath = "/uploads/images/$storedFileName"

    val targetPath = imageProperties.uploadDirPath.resolve(storedFileName)
    filePart.transferTo(targetPath).awaitSingleOrNull()

    val fileSize = File(targetPath.toString()).length()
    if (!imageProperties.isValidFileSize(fileSize)) {
      File(targetPath.toString()).delete()
      throw ImageError.FileSizeExceeded(imageProperties.getMaxFileSize(), fileSize)
    }

    return ImageEntity
      .from(
        UploadImageRequestDto(
          userId = userId,
          originalFileName = originalFileName,
          storedFileName = storedFileName,
          filePath = filePath,
          fileSize = fileSize,
          contentType = filePart.headers().contentType?.toString() ?: "application/octet-stream",
        ),
      ).let { repository.save(it) }
      .let { Image.from(it) }
  }

  private fun validateFileType(filePart: FilePart) {
    val contentType = filePart.headers().contentType?.toString()

    if (!imageProperties.isValidFileType(contentType)) {
      throw ImageError.UnsupportedFileType(imageProperties.getFormattedAllowedTypes())
    }
  }

  private fun getFileExtension(filename: String): String {
    val lastDotIndex = filename.lastIndexOf('.')
    return if (lastDotIndex > 0 && lastDotIndex < filename.length - 1) {
      filename.substring(lastDotIndex)
    } else {
      ""
    }
  }
}
