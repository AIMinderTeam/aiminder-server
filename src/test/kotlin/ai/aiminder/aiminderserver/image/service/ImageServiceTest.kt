package ai.aiminder.aiminderserver.image.service

import ai.aiminder.aiminderserver.image.entity.ImageEntity
import ai.aiminder.aiminderserver.image.error.ImageError
import ai.aiminder.aiminderserver.image.property.ImageProperties
import ai.aiminder.aiminderserver.image.repository.ImageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import reactor.core.publisher.Mono
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

class ImageServiceTest {
  private lateinit var imageRepository: ImageRepository
  private lateinit var imageService: ImageService
  private lateinit var mockFilePart: FilePart
  private lateinit var mockHeaders: HttpHeaders

  private val testUserId = UUID.randomUUID()

  @BeforeEach
  fun setUp() {
    imageRepository = mockk()
    mockFilePart = mockk()
    mockHeaders = mockk()

    val imageProperties = ImageProperties()
    imageService =
      ImageService(
        repository = imageRepository,
        imageProperties = imageProperties,
      )

    imageProperties.uploadDirPath.toFile().deleteRecursively()
  }

  @Test
  fun `이미지 업로드 성공 테스트`() =
    runTest {
      // given
      val filename = "test.jpg"
      val contentType = MediaType.IMAGE_JPEG
      val savedEntity =
        ImageEntity(
          id = UUID.randomUUID(),
          userId = testUserId,
          originalFileName = filename,
          storedFileName = "stored-file.jpg",
          filePath = "/uploads/images/stored-file.jpg",
          fileSize = 12345,
          contentType = contentType.toString(),
          createdAt = Instant.now(),
          updatedAt = Instant.now(),
        )

      coEvery { mockFilePart.filename() } returns filename
      coEvery { mockFilePart.headers() } returns mockHeaders
      coEvery { mockHeaders.contentType } returns contentType
      coEvery { mockFilePart.transferTo(any<Path>()) } returns Mono.empty()
      coEvery { imageRepository.save(any<ImageEntity>()) } returns savedEntity

      // when
      val result = imageService.uploadImage(mockFilePart, testUserId)

      // then
      assertThat(result.id).isEqualTo(savedEntity.id)
      assertThat(result.originalFileName).isEqualTo(filename)
      assertThat(result.userId).isEqualTo(testUserId)
      assertThat(result.contentType).isEqualTo(contentType.toString())

      val entitySlot = slot<ImageEntity>()
      coVerify { imageRepository.save(capture(entitySlot)) }
      assertThat(entitySlot.captured.originalFileName).isEqualTo(filename)
      assertThat(entitySlot.captured.userId).isEqualTo(testUserId)
    }

  @Test
  fun `지원하지 않는 파일 타입 업로드 시 예외 발생`() =
    runTest {
      // given
      val filename = "test.txt"
      val contentType = MediaType.TEXT_PLAIN

      coEvery { mockFilePart.filename() } returns filename
      coEvery { mockFilePart.headers() } returns mockHeaders
      coEvery { mockHeaders.contentType } returns contentType

      // when & then
      assertThatThrownBy {
        runBlocking {
          imageService.uploadImage(mockFilePart, testUserId)
        }
      }.isInstanceOf(ImageError.UnsupportedFileType::class.java)
        .hasMessageContaining(
          "지원하지 않는 파일 포맷이며 다음 파일 포맷만 지원합니다 : " +
            "image/jpg, image/jpeg, image/png, image/gif, image/web",
        )
    }
}
