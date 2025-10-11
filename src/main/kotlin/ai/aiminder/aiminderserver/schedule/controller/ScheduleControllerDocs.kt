package ai.aiminder.aiminderserver.schedule.controller

import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.schedule.dto.CreateScheduleRequest
import ai.aiminder.aiminderserver.schedule.dto.GetSchedulesRequest
import ai.aiminder.aiminderserver.schedule.dto.ScheduleResponse
import ai.aiminder.aiminderserver.schedule.dto.UpdateScheduleRequest
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
 * ScheduleController에 대한 어노테이션 기반 Swagger 문서 인터페이스.
 * 컨트롤러가 본 인터페이스를 구현하면, 메서드 시그니처/어노테이션이 문서에 반영됩니다.
 */
@Tag(name = "Schedule", description = "일정 관리 API")
interface ScheduleControllerDocs {
  @Operation(
    operationId = "createSchedule",
    summary = "새로운 일정 생성",
    description =
      "새로운 일정을 생성합니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "인증 정보가 없거나 유효하지 않으면 401이 반환됩니다. " +
        "startDate와 endDate는 ISO 8601 날짜 형식(예: 2024-03-15)으로 입력해야 합니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공: 일정 생성 완료",
      ),
      ApiResponse(
        responseCode = "400",
        description = "요청 데이터 검증 실패: 필수 필드 누락 또는 잘못된 날짜",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 400,
                  "message": "시작 날짜는 종료 날짜보다 늦을 수 없습니다.",
                  "errorCode": "COMMON:INVALIDREQUEST",
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
  suspend fun createSchedule(
    goalId: UUID,
    request: CreateScheduleRequest,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<ScheduleResponse>

  @Operation(
    operationId = "getSchedules",
    summary = "일정 목록 조회",
    description =
      "사용자의 일정 목록을 조회합니다. " +
        "goalId, status, 날짜 범위로 필터링할 수 있으며, 페이지네이션을 지원합니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공: 일정 목록 조회 완료",
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
  suspend fun getSchedules(
    goalId: UUID,
    request: GetSchedulesRequest,
    pageable: PageableRequest,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<List<ScheduleResponse>>

  @Operation(
    operationId = "updateSchedule",
    summary = "일정 수정",
    description = "기존 일정을 수정합니다. 소유자만 수정할 수 있습니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공: 일정 수정 완료",
      ),
      ApiResponse(
        responseCode = "400",
        description = "요청 데이터 검증 실패: 필수 필드 누락 또는 잘못된 날짜",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 400,
                  "message": "시작 날짜는 종료 날짜보다 늦을 수 없습니다.",
                  "errorCode": "COMMON:INVALIDREQUEST",
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
        responseCode = "403",
        description = "권한 없음: 일정의 소유자가 아닌 경우",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 403,
                  "message": "해당 일정을 수정할 권한이 없습니다.",
                  "errorCode": "AUTH:FORBIDDEN",
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
        description = "일정을 찾을 수 없음: 존재하지 않는 일정 ID",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 404,
                  "message": "요청한 일정을 찾을 수 없습니다.",
                  "errorCode": "SCHEDULE:NOTFOUND",
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
  suspend fun updateSchedule(
    @Parameter(description = "수정할 일정 ID") scheduleId: UUID,
    request: UpdateScheduleRequest,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<ScheduleResponse>

  @Operation(
    operationId = "deleteSchedule",
    summary = "일정 삭제",
    description = "일정을 삭제합니다(소프트 삭제). 소유자만 삭제할 수 있습니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공: 일정 삭제 완료",
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
        responseCode = "403",
        description = "권한 없음: 일정의 소유자가 아닌 경우",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 403,
                  "message": "해당 일정을 삭제할 권한이 없습니다.",
                  "errorCode": "AUTH:FORBIDDEN",
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
        description = "일정을 찾을 수 없음: 존재하지 않는 일정 ID",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 404,
                  "message": "요청한 일정을 찾을 수 없습니다.",
                  "errorCode": "SCHEDULE:NOTFOUND",
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
  suspend fun deleteSchedule(
    @Parameter(description = "삭제할 일정 ID") scheduleId: UUID,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<String>

  @Operation(
    operationId = "getScheduleById",
    summary = "일정 상세 조회",
    description = "특정 일정의 상세 정보를 조회합니다. 소유자만 조회할 수 있습니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공: 일정 상세 정보 조회 완료",
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
        responseCode = "403",
        description = "권한 없음: 일정의 소유자가 아닌 경우",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 403,
                  "message": "해당 일정을 조회할 권한이 없습니다.",
                  "errorCode": "AUTH:FORBIDDEN",
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
        description = "일정을 찾을 수 없음: 존재하지 않는 일정 ID",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 404,
                  "message": "요청한 일정을 찾을 수 없습니다.",
                  "errorCode": "SCHEDULE:NOTFOUND",
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
  suspend fun getScheduleById(
    @Parameter(description = "조회할 일정 ID") scheduleId: UUID,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<ScheduleResponse>
}
