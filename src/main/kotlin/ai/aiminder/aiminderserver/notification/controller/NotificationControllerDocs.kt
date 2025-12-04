package ai.aiminder.aiminderserver.notification.controller

import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.notification.domain.Notification
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
import java.util.UUID

/**
 * NotificationController에 대한 어노테이션 기반 Swagger 문서 인터페이스.
 * 컨트롤러가 본 인터페이스를 구현하면, 메서드 시그니처/어노테이션이 문서에 반영됩니다.
 */
@Tag(name = "Notification", description = "알림 관리 API")
interface NotificationControllerDocs {
  @Operation(
    operationId = "getCountOfUncheckedNotifications",
    summary = "읽지 않은 알림 개수 조회",
    description =
      "사용자의 읽지 않은 알림 개수를 조회합니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "인증 정보가 없거나 유효하지 않으면 401이 반환됩니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공적으로 알림 개수 조회 완료",
      ),
      ApiResponse(
        responseCode = "401",
        description = "토큰이 없거나 유효하지 않음",
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
        description = "데이터베이스 연결 실패 등 서버 내부 오류",
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
  suspend fun getCountOfUncheckedNotifications(
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<Int>

  @Operation(
    operationId = "getNotifications",
    summary = "알림 목록 조회",
    description =
      "사용자의 알림 목록을 페이지네이션으로 조회합니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "인증 정보가 없거나 유효하지 않으면 401이 반환됩니다. " +
        "모든 알림(읽음/읽지 않음 포함)이 조회되며, 삭제된 알림은 제외됩니다. 생성일 기준 내림차순으로 정렬됩니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공적으로 알림 목록 조회 완료",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 200,
                  "message": "요청이 성공했습니다.",
                  "errorCode": null,
                  "data": [
                    {
                      "id": "123e4567-e89b-12d3-a456-426614174000",
                      "type": "ASSISTANT_FEEDBACK",
                      "title": "AI 비서 알림",
                      "description": "\"운동 목표\" 목표에 대한 피드백을 확인하세요.",
                      "metadata": {
                        "receiverId": "789e0123-e89b-12d3-a456-426614174002",
                        "goalTitle": "운동 목표",
                        "conversationId": "456e7890-e89b-12d3-a456-426614174001",
                        "type": "ASSISTANT_FEEDBACK"
                      },
                      "receiverId": "789e0123-e89b-12d3-a456-426614174002",
                      "checked": false,
                      "createdAt": "2024-10-14T10:30:00Z",
                      "updatedAt": "2024-10-14T10:30:00Z",
                      "deletedAt": null
                    }
                  ],
                  "pageable": {
                    "page": 0,
                    "count": 1,
                    "totalElements": 1,
                    "totalPages": 1
                  }
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
  suspend fun getNotifications(
    @Parameter(description = "페이지네이션 정보 (page: 페이지 번호, size: 페이지 크기, sort: 정렬 필드, direction: 정렬 방향)")
    pageable: PageableRequest,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<List<Notification>>

  @Operation(
    operationId = "checkNotification",
    summary = "개별 알림 확인",
    description =
      "특정 알림을 확인 상태로 변경합니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "인증 정보가 없거나 유효하지 않으면 401이 반환됩니다. " +
        "존재하지 않는 알림이거나 다른 사용자의 알림에 접근하려고 하면 404가 반환됩니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공적으로 알림 확인 완료",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 200,
                  "message": "요청이 성공했습니다.",
                  "errorCode": null,
                  "data": {
                    "id": "123e4567-e89b-12d3-a456-426614174000",
                    "type": "ASSISTANT_FEEDBACK",
                    "title": "AI 비서 알림",
                    "description": "\"운동 목표\" 목표에 대한 피드백을 확인하세요.",
                    "metadata": {
                      "receiverId": "789e0123-e89b-12d3-a456-426614174002",
                      "goalTitle": "운동 목표",
                      "conversationId": "456e7890-e89b-12d3-a456-426614174001",
                      "type": "ASSISTANT_FEEDBACK"
                    },
                    "receiverId": "789e0123-e89b-12d3-a456-426614174002",
                    "checked": true,
                    "createdAt": "2024-10-14T10:30:00Z",
                    "updatedAt": "2024-10-14T10:35:00Z",
                    "deletedAt": null
                  },
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
        responseCode = "404",
        description = "알림을 찾을 수 없음: 존재하지 않는 알림이거나 권한이 없음",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 404,
                  "message": "알림을 찾을 수 없습니다.",
                  "errorCode": "NOTIFICATION:NOTFOUND",
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
  suspend fun checkNotification(
    @Parameter(description = "확인할 알림의 고유 식별자", required = true)
    notificationId: UUID,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<Notification>

  @Operation(
    operationId = "checkNotifications",
    summary = "모든 알림 일괄 확인",
    description =
      "사용자의 모든 미확인 알림을 일괄적으로 확인 상태로 변경합니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "인증 정보가 없거나 유효하지 않으면 401이 반환됩니다. " +
        "삭제된 알림이나 이미 확인된 알림은 처리되지 않습니다. " +
        "확인된 알림 목록이 반환되며, 확인할 알림이 없는 경우 빈 배열이 반환됩니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공적으로 모든 알림 일괄 확인 완료",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 200,
                  "message": "요청이 성공했습니다.",
                  "errorCode": null,
                  "data": [
                    {
                      "id": "123e4567-e89b-12d3-a456-426614174000",
                      "type": "ASSISTANT_FEEDBACK",
                      "title": "AI 비서 알림",
                      "description": "\"운동 목표\" 목표에 대한 피드백을 확인하세요.",
                      "metadata": {
                        "receiverId": "789e0123-e89b-12d3-a456-426614174002",
                        "goalTitle": "운동 목표",
                        "conversationId": "456e7890-e89b-12d3-a456-426614174001",
                        "type": "ASSISTANT_FEEDBACK"
                      },
                      "receiverId": "789e0123-e89b-12d3-a456-426614174002",
                      "checked": true,
                      "createdAt": "2024-10-14T10:30:00Z",
                      "updatedAt": "2024-10-14T10:35:00Z",
                      "deletedAt": null
                    },
                    {
                      "id": "234e5678-e89b-12d3-a456-426614174003",
                      "type": "ASSISTANT_FEEDBACK",
                      "title": "AI 비서 알림",
                      "description": "\"독서 목표\" 목표에 대한 피드백을 확인하세요.",
                      "metadata": {
                        "receiverId": "789e0123-e89b-12d3-a456-426614174002",
                        "goalTitle": "독서 목표",
                        "conversationId": "567e8901-e89b-12d3-a456-426614174004",
                        "type": "ASSISTANT_FEEDBACK"
                      },
                      "receiverId": "789e0123-e89b-12d3-a456-426614174002",
                      "checked": true,
                      "createdAt": "2024-10-14T10:25:00Z",
                      "updatedAt": "2024-10-14T10:35:00Z",
                      "deletedAt": null
                    }
                  ],
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
  suspend fun checkNotifications(
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<List<Notification>>
}
