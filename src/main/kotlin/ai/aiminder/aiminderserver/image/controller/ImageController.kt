package ai.aiminder.aiminderserver.image.controller

import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.image.dto.ImageResponse
import ai.aiminder.aiminderserver.image.service.ImageService
import ai.aiminder.aiminderserver.user.domain.User
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/images")
class ImageController(
  private val imageService: ImageService,
) : ImageControllerDocs {
  @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  override suspend fun uploadImage(
    @RequestPart("file") file: FilePart,
    @AuthenticationPrincipal user: User,
  ): ServiceResponse<ImageResponse> =
    imageService
      .uploadImage(file, user.id)
      .let { image -> ImageResponse.from(image) }
      .let { response -> ServiceResponse.from(response) }
}
