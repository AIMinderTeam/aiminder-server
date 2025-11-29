package ai.aiminder.aiminderserver.assistant.controller

import ai.aiminder.aiminderserver.assistant.dto.AssistantRequest
import ai.aiminder.aiminderserver.assistant.dto.ChatResponse
import ai.aiminder.aiminderserver.common.request.PageableRequest
import ai.aiminder.aiminderserver.common.response.ServiceResponse
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
 * AssistantController에 대한 어노테이션 기반 Swagger 문서 인터페이스.
 * 컨트롤러가 본 인터페이스를 구현하면, 메서드 시그니처/어노테이션이 문서에 반영됩니다.
 */
@Tag(name = "Assistant", description = "AI 어시스턴트 채팅 API")
interface AssistantControllerDocs {
  @Operation(
    operationId = "startChat",
    summary = "새로운 AI 대화 시작",
    description =
      "새로운 AI 어시스턴트와의 대화 세션을 시작합니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "인증 정보가 없거나 유효하지 않으면 401이 반환됩니다. " +
        "자동으로 새로운 대화방이 생성되고 AI 어시스턴트의 환영 메시지가 반환됩니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공: 새로운 대화 시작 완료",
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
        description = "서버 내부 오류: 대화방 생성 실패 또는 AI 서비스 연결 오류",
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
  suspend fun startChat(
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<ChatResponse>

  @Operation(
    operationId = "sendMessage",
    summary = "AI 어시스턴트에게 메시지 전송",
    description =
      "기존 대화방에 사용자 메시지를 전송하고 AI 응답을 받습니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "대화방 소유자만 접근 가능하며, 빈 메시지는 전송할 수 없습니다. " +
        "AI 어시스턴트가 실시간으로 응답을 생성하여 반환합니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공: 메시지 전송 및 AI 응답 완료",
      ),
      ApiResponse(
        responseCode = "400",
        description = "잘못된 요청: 빈 메시지 전송 시",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 400,
                  "message": "메시지 내용이 비어있습니다.",
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
        description = "권한 없음: 대화방 접근 권한이 없는 경우",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 403,
                  "message": "해당 대화방에 접근할 권한이 없습니다.",
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
        description = "대화방을 찾을 수 없음: 존재하지 않는 conversationId",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 404,
                  "message": "요청한 대화방을 찾을 수 없습니다.",
                  "errorCode": "CONVERSATION:NOTFOUND",
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
        description = "서버 내부 오류: AI 서비스 연결 오류 또는 메시지 처리 실패",
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
  suspend fun sendMessage(
    @Parameter(description = "대화방 고유 ID (UUID)") conversationId: UUID,
    @Parameter(description = "전송할 메시지 내용") request: AssistantRequest,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<ChatResponse>

  @Operation(
    operationId = "getMessages",
    summary = "대화방 메시지 목록 조회",
    description =
      "특정 대화방의 메시지 목록을 페이지네이션으로 조회합니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "대화방 소유자만 접근 가능하며, 최신 메시지부터 내림차순으로 정렬되어 반환됩니다. " +
        "페이지네이션을 통해 대용량 메시지 목록을 효율적으로 조회할 수 있습니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공: 메시지 목록 조회 완료",
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
        description = "권한 없음: 대화방 접근 권한이 없는 경우",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 403,
                  "message": "해당 대화방에 접근할 권한이 없습니다.",
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
        description = "대화방을 찾을 수 없음: 존재하지 않는 conversationId",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 404,
                  "message": "요청한 대화방을 찾을 수 없습니다.",
                  "errorCode": "CONVERSATION:NOTFOUND",
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
        description = "서버 내부 오류: 메시지 조회 실패 또는 데이터베이스 연결 오류",
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
  suspend fun getMessages(
    @Parameter(description = "대화방 고유 ID (UUID)") conversationId: UUID,
    @Parameter(description = "페이지네이션 정보 (page: 페이지 번호, size: 페이지 크기, sort: 정렬 필드, direction: 정렬 방향)")
    pageable: PageableRequest,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<List<ChatResponse>>

  @Operation(
    operationId = "feedback",
    summary = "AI 피드백 요청",
    description =
      "특정 대화방에 대해 AI 피드백을 요청합니다. " +
        "어제와 오늘의 일정을 바탕으로 AI가 목표 달성을 위한 피드백을 제공합니다. " +
        "OAuth2 로그인 성공 시 설정되는 `ACCESS_TOKEN`(필수) / `REFRESH_TOKEN`(선택) 쿠키 기반 인증 또는 " +
        "Authorization 헤더의 Bearer 토큰 인증을 사용합니다. " +
        "대화방 소유자만 접근 가능하며, 목표가 설정된 대화방에서만 피드백을 받을 수 있습니다.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "성공: 피드백 생성 완료",
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
                    "conversationId": "550e8400-e29b-41d4-a716-446655440000",
                    "chatType": "ASSISTANT",
                    "chat": [
                      {
                        "type": "TEXT",
                        "messages": [
                          "어제와 오늘의 일정을 바탕으로 피드백을 드리겠습니다. 오늘 목표 달성을 위해 어제 계획한 운동을 완료하지 못한 것 같습니다. 내일은 더 구체적인 시간 계획을 세워보는 것을 추천합니다."
                        ]
                      }
                    ]
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
        responseCode = "403",
        description = "권한 없음: 대화방 접근 권한이 없는 경우",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 403,
                  "message": "해당 대화방에 접근할 권한이 없습니다.",
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
        description = "대화방을 찾을 수 없음: 존재하지 않는 conversationId",
        content = [
          Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema =
              Schema(
                example = """
                {
                  "statusCode": 404,
                  "message": "요청한 대화방을 찾을 수 없습니다.",
                  "errorCode": "CONVERSATION:NOTFOUND",
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
        description = "서버 내부 오류: 피드백 생성 실패",
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
  suspend fun feedback(
    @Parameter(description = "대화방 고유 ID (UUID)") conversationId: UUID,
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<ChatResponse>
}
