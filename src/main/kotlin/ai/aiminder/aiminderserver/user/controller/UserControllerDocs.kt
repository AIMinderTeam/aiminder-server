package ai.aiminder.aiminderserver.user.controller

import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.dto.WithdrawUserRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType

@Tag(name = "User", description = "사용자 계정 관리 API")
interface UserControllerDocs {
  @Operation(
    operationId = "withdrawUser",
    summary = "회원 탈퇴",
    description = "현재 로그인한 사용자의 계정을 탈퇴 처리합니다. 탈퇴 시 모든 데이터가 삭제되며 복구가 불가능합니다.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공: 회원 탈퇴 완료",
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
                implementation = ServiceResponse::class,
              ),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "사용자를 찾을 수 없음",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 404,
                  "message": "사용자를 찾을 수 없습니다.",
                  "errorCode": "AUTH:USER_NOT_FOUND",
                  "data": null,
                  "pageable": null
                }
              """,
                implementation = ServiceResponse::class,
              ),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 500,
                  "message": "서버 내부 오류가 발생했습니다.",
                  "errorCode": "COMMON:INTERNALSERVERERROR",
                  "data": null,
                  "pageable": null
                }
              """,
                implementation = ServiceResponse::class,
              ),
          ),
        ],
      ),
    ],
  )
  suspend fun withdrawUser(
    request: WithdrawUserRequest,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<Unit>
}
