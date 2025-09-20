package ai.aiminder.aiminderserver.image.service

import ai.aiminder.aiminderserver.image.domain.Image
import ai.aiminder.aiminderserver.image.dto.UploadImageRequestDto
import ai.aiminder.aiminderserver.image.entity.ImageEntity
import ai.aiminder.aiminderserver.image.error.ImageError
import ai.aiminder.aiminderserver.image.repository.ImageRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Paths
import java.util.UUID

@Service
class ImageService(
  private val repository: ImageRepository,
  @param:Value("\${app.file.upload-dir:./uploads/images}")
  private val uploadDir: String,
  @param:Value("\${app.file.max-file-size:5242880}")
  private val maxFileSize: Long,
  @param:Value("\${app.file.allowed-types:image/jpeg,image/png,image/gif,image/webp}")
  private val allowedTypes: String,
) {
  init {
    File(uploadDir).mkdirs()
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

    val targetPath = Paths.get(uploadDir, storedFileName)
    filePart.transferTo(targetPath).block()

    val fileSize = File(targetPath.toString()).length()
    if (fileSize > maxFileSize) {
      File(targetPath.toString()).delete()
      throw ImageError.FileSizeExceeded(maxFileSize, fileSize)
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
    val allowedTypesList = allowedTypes.split(",").map { it.trim() }

    if (contentType == null || !allowedTypesList.contains(contentType)) {
      throw ImageError.UnsupportedFileType(allowedTypesList.joinToString(", "))
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
