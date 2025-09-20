package ai.aiminder.aiminderserver.image.controller

import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.image.dto.ImageResponse
import ai.aiminder.aiminderserver.user.domain.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart

/**
 * ImageController에 대한 어노테이션 기반 Swagger 문서 인터페이스.
 * 컨트롤러가 본 인터페이스를 구현하면, 메서드 시그니처/어노테이션이 문서에 반영됩니다.
 */
@Tag(name = "Image", description = "이미지 관리 API")
interface ImageControllerDocs {
  @Operation(
    operationId = "uploadImage",
    summary = "이미지 업로드",
    description =
      "이미지 파일을 업로드합니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "인증 정보가 없거나 유효하지 않으면 401이 반환됩니다. " +
        "지원되는 파일 형식: JPEG, PNG, GIF, WebP. 최대 파일 크기: 5MB",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공: 이미지 업로드 완료",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 200,
                  "message": null,
                  "errorCode": null,
                  "data": {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "userId": "2f6a3a4c-1c3b-4bde-9d2a-6c2c8b6a1e7f",
                    "originalFileName": "photo.jpg",
                    "storedFileName": "550e8400-e29b-41d4-a716-446655440000.jpg",
                    "filePath": "/uploads/images/550e8400-e29b-41d4-a716-446655440000.jpg",
                    "fileSize": 1024000,
                    "contentType": "image/jpeg",
                    "createdAt": "2024-01-01T10:00:00Z",
                    "updatedAt": "2024-01-01T10:00:00Z",
                    "deletedAt": null
                  },
                  "pageable": null
                }
              """,
              ),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "요청 데이터 검증 실패: 지원하지 않는 파일 형식 또는 파일 크기 초과",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 400,
                  "message": "지원하지 않는 파일 형식입니다. image/jpeg, image/png, image/gif, image/webp 파일만 업로드 가능합니다.",
                  "errorCode": "IMAGE:UNSUPPORTED_FILE_TYPE",
                  "data": null,
                  "pageable": null
                }
              """,
              ),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "인증 실패: 토큰이 없거나 유효하지 않음",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 401,
                  "message": "인증이 필요합니다. 로그인을 진행해주세요.",
                  "errorCode": "AUTH:UNAUTHORIZED",
                  "data": null,
                  "pageable": null
                }
              """,
              ),
          ),
        ],
      ),
    ],
  )
  suspend fun uploadImage(
    @Parameter(description = "업로드할 이미지 파일") file: FilePart,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<ImageResponse>
}
