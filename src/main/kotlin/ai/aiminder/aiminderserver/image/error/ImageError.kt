package ai.aiminder.aiminderserver.image.error

import ai.aiminder.aiminderserver.common.error.ServiceError
import org.springframework.http.HttpStatus

sealed class ImageError(
  override val status: HttpStatus,
  override val message: String,
) : ServiceError() {
  override val mainCode: String = "IMAGE"

  class UnsupportedFileType(
    message: String,
  ) : ImageError(
      HttpStatus.BAD_REQUEST,
      "지원하지 않는 파일 포맷이며 다음 파일 포맷만 지원합니다 : $message",
    )

  class FileSizeExceeded(
    maxFileSize: Long,
    fileSize: Long,
  ) : ImageError(
      HttpStatus.BAD_REQUEST,
      "허용된 최대 파일 크기($maxFileSize)보다 큰 파일($fileSize)입니다.",
    )
}
