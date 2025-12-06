package ai.aiminder.aiminderserver.user.controller

import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.user.domain.User
import ai.aiminder.aiminderserver.user.dto.GetUserNotificationSettingsResponse
import ai.aiminder.aiminderserver.user.dto.UpdateAiFeedbackEnabledRequest
import ai.aiminder.aiminderserver.user.dto.UpdateAiFeedbackNotificationTimeRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType

/**
 * UserNotificationSettingsController에 대한 어노테이션 기반 Swagger 문서 인터페이스.
 * 컨트롤러가 본 인터페이스를 구현하면, 메서드 시그니처/어노테이션이 문서에 반영됩니다.
 */
@Tag(name = "User Notification Settings", description = "사용자 알림 설정 관리 API")
interface UserNotificationSettingsControllerDocs {
  @Operation(
    operationId = "getNotificationSettings",
    summary = "사용자 알림 설정 조회",
    description =
      "사용자의 알림 설정을 조회합니다. " +
        "설정이 없는 경우 기본 설정(AI 피드백 활성화, 알림 시간 09:00)이 자동으로 생성됩니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "인증 정보가 없거나 유효하지 않으면 401이 반환됩니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공적으로 알림 설정 조회 완료",
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
        responseCode = "500",
        description = "서버 내부 오류: 데이터베이스 연결 실패 등",
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
  suspend fun getNotificationSettings(
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<GetUserNotificationSettingsResponse>

  @Operation(
    operationId = "updateAiFeedbackEnabled",
    summary = "AI 피드백 활성화 설정 업데이트",
    description =
      "AI 피드백 기능의 활성화 여부를 업데이트합니다. " +
        "설정이 없는 경우 기본 설정이 자동으로 생성되며, AI 피드백 알림 시간은 기존 값이 유지됩니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "인증 정보가 없거나 유효하지 않으면 401이 반환됩니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공적으로 AI 피드백 활성화 설정 업데이트 완료",
      ),
      ApiResponse(
        responseCode = "400",
        description = "요청 데이터 검증 실패: 필수 필드 누락 또는 잘못된 형식",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 400,
                  "message": "요청 데이터가 유효하지 않습니다. aiFeedbackEnabled 필드는 필수입니다.",
                  "errorCode": "VALIDATION:FAILED",
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
        responseCode = "500",
        description = "서버 내부 오류: 데이터베이스 연결 실패 등",
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
  suspend fun updateAiFeedbackEnabled(
    request: UpdateAiFeedbackEnabledRequest,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<GetUserNotificationSettingsResponse>

  @Operation(
    operationId = "updateAiFeedbackNotificationTime",
    summary = "AI 피드백 알림 시간 업데이트",
    description =
      "AI 피드백 알림이 발송될 시간을 업데이트합니다. " +
        "시간은 HH:mm 형식으로 입력해야 하며, 24시간 형식을 사용합니다. " +
        "설정이 없는 경우 기본 설정이 자동으로 생성되며, AI 피드백 활성화 여부는 기존 값이 유지됩니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "인증 정보가 없거나 유효하지 않으면 401이 반환됩니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공적으로 AI 피드백 알림 시간 업데이트 완료",
      ),
      ApiResponse(
        responseCode = "400",
        description = "요청 데이터 검증 실패: 필수 필드 누락 또는 잘못된 시간 형식",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 400,
                  "message": "요청 데이터가 유효하지 않습니다. 시간 형식이 올바르지 않습니다. HH:mm 형식으로 입력해주세요.",
                  "errorCode": "VALIDATION:FAILED",
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
        responseCode = "500",
        description = "서버 내부 오류: 데이터베이스 연결 실패 등",
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
  suspend fun updateAiFeedbackNotificationTime(
    request: UpdateAiFeedbackNotificationTimeRequest,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<GetUserNotificationSettingsResponse>
}
