package ai.aiminder.aiminderserver.assistant.controller

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.dto.AssistantRequest
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
                    "responses": [
                      {
                        "type": "TEXT",
                        "messages": ["안녕하세요! 👋 저는 당신의 AI 비서입니다. 지금부터 목표 달성 여정을 함께 설계해 봐요. 먼저 목표를 명확히 파악해야 해요. 🎯 이루고자 하는 목표는 무엇인가요❓"]
                      },
                      {
                        "type": "QUICK_REPLIES",
                        "messages" : [ "다이어트 💪", "경제적 자유 💰", "자격증 취득 🏅" ]
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
              ),
          ),
        ],
      ),
    ],
  )
  suspend fun startChat(
    @Parameter(hidden = true) user: User,
  ): ServiceResponse<AssistantResponse>

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
                    "responses": [
                      {
                        "type": "TEXT",
                        "messages": ["안녕하세요! 목표 설정이나 일정 관리에 도움을 드릴 수 있습니다. 어떤 도움이 필요하신가요?"]
                      },
                      {
                        "type": "QUICK_REPLIES",
                        "messages": ["목표 설정", "일정 관리"]
                      }
                    ]
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
  ): ServiceResponse<AssistantResponse>
}
