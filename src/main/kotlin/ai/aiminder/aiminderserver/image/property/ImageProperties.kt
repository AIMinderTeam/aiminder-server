package ai.aiminder.aiminderserver.image.property

import org.springframework.boot.context.properties.ConfigurationProperties
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@ConfigurationProperties(prefix = "aiminder.image")
data class ImageProperties(
  private val uploadDir: String = "./uploads/images",
  private val maxFileSize: Long = 5242880,
  private val allowedTypes: String = "image/jpeg,image/png,image/gif,image/webp",
) {
  val uploadDirPath: Path = Paths.get(uploadDir)
  val allowedTypesList: List<String> = allowedTypes.split(",").map { it.trim() }

  fun createUploadDirectory() {
    File(uploadDir).mkdirs()
  }

  fun isValidFileSize(fileSize: Long): Boolean = fileSize <= maxFileSize

  fun getMaxFileSize(): Long = maxFileSize

  fun isValidFileType(contentType: String?): Boolean {
    return contentType != null && allowedTypesList.contains(contentType)
  }

  fun getFormattedAllowedTypes(): String = allowedTypesList.joinToString(", ")
}
