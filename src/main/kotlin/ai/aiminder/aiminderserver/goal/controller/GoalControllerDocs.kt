package ai.aiminder.aiminderserver.goal.controller

import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
import ai.aiminder.aiminderserver.goal.dto.CreateGoalRequest
import ai.aiminder.aiminderserver.goal.dto.GetGoalsRequest
import ai.aiminder.aiminderserver.goal.dto.GoalResponse
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

/**
 * GoalController에 대한 어노테이션 기반 Swagger 문서 인터페이스.
 * 컨트롤러가 본 인터페이스를 구현하면, 메서드 시그니처/어노테이션이 문서에 반영됩니다.
 */
@Tag(name = "Goal", description = "목표 관리 API")
interface GoalControllerDocs {
  @Operation(
    operationId = "createGoal",
    summary = "새로운 목표 생성",
    description =
      "새로운 목표를 생성합니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "인증 정보가 없거나 유효하지 않으면 401이 반환됩니다. " +
        "targetDate는 ISO 8601 형식(예: 2024-03-15T10:30:00Z)으로 입력해야 합니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공: 목표 생성 완료",
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
                    "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                    "userId": "2f6a3a4c-1c3b-4bde-9d2a-6c2c8b6a1e7f",
                    "title": "Learn Kotlin",
                    "description": "Master Kotlin programming language by reading documentation and building projects",
                    "targetDate": "2024-04-15T00:00:00Z",
                    "isAiGenerated": false,
                    "status": "ACTIVE",
                    "imagePath": "/uploads/images/kotlin-learning-image.jpg",
                    "createdAt": "2024-03-15T10:30:00Z",
                    "updatedAt": "2024-03-15T10:30:00Z",
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
        description = "요청 데이터 검증 실패: 필수 필드 누락 또는 잘못된 날짜 형식",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 400,
                  "message": "잘못된 요청 데이터입니다. 필수 필드를 확인해주세요.",
                  "errorCode": "COMMON:INVALIDREQUEST",
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
      ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류: 존재하지 않는 사용자로 요청 시 등",
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
              ),
          ),
        ],
      ),
    ],
  )
  suspend fun createGoal(
    request: CreateGoalRequest,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<GoalResponse>

  @Operation(
    operationId = "getGoals",
    summary = "목표 목록 조회",
    description =
      "사용자의 목표 목록을 조회합니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "인증 정보가 없거나 유효하지 않으면 401이 반환됩니다. " +
        "status로 목표 상태를 필터링할 수 있으며, 페이지네이션을 지원합니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공: 목표 목록 조회 완료",
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
                  "data": [
                    {
                      "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                      "userId": "2f6a3a4c-1c3b-4bde-9d2a-6c2c8b6a1e7f",
                      "title": "Learn Kotlin",
                      "description": "Master Kotlin programming language by reading documentation and building projects",
                      "targetDate": "2024-04-15T00:00:00Z",
                      "isAiGenerated": false,
                      "status": "ACTIVE",
                      "imagePath": "/uploads/images/kotlin-learning-image.jpg",
                      "createdAt": "2024-03-15T10:30:00Z",
                      "updatedAt": "2024-03-15T10:30:00Z",
                      "deletedAt": null
                    },
                    {
                      "id": "a12bc34d-56ef-7890-abcd-ef1234567890",
                      "userId": "2f6a3a4c-1c3b-4bde-9d2a-6c2c8b6a1e7f",
                      "title": "Complete Spring Boot Project",
                      "description": "Build a complete REST API using Spring Boot with JWT authentication",
                      "targetDate": "2024-05-01T00:00:00Z",
                      "isAiGenerated": true,
                      "status": "ACTIVE",
                      "imagePath": null,
                      "createdAt": "2024-03-16T14:20:00Z",
                      "updatedAt": "2024-03-16T14:20:00Z",
                      "deletedAt": null
                    }
                  ],
                  "pageable": {
                    "page": 0,
                    "count": 2,
                    "totalPages": 1,
                    "totalElements": 2
                  }
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
                  "data": null
                }
              """,
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
                  "data": null
                }
              """,
              ),
          ),
        ],
      ),
    ],
  )
  suspend fun getGoals(
    @Parameter(description = "목표 조회 필터 (status: 목표 상태 - ACTIVE, COMPLETED, ARCHIVED)")
    request: GetGoalsRequest,
    @Parameter(description = "페이지네이션 정보 (page: 페이지 번호, size: 페이지 크기, sort: 정렬 필드, direction: 정렬 방향)")
    pageable: PageableRequest,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<List<GoalResponse>>
}
