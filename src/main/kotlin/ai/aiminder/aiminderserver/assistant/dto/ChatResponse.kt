package ai.aiminder.aiminderserver.assistant.dto

import ai.aiminder.aiminderserver.assistant.domain.AssistantResponse
import ai.aiminder.aiminderserver.assistant.domain.AssistantResponseType
import ai.aiminder.aiminderserver.assistant.domain.ChatResponseDto
import ai.aiminder.aiminderserver.assistant.domain.ChatType
import ai.aiminder.aiminderserver.assistant.entity.ChatEntity
import ai.aiminder.aiminderserver.assistant.error.AssistantError
import ai.aiminder.aiminderserver.conversation.domain.Conversation
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "AI 어시스턴트 응답 데이터")
data class ChatResponse(
  @Schema(description = "대화방 식별자")
  val conversationId: UUID,
  @Schema(
    description = "응답 메시지 목록",
    example =
      "[{\"type\": \"TEXT\",\"messages\": [\"안녕하세요!\" ] }, " +
        "{\"type\": \"QUICK_REPLIES\",\"messages\": " +
        "[\"다이어트 \uD83D\uDCAA\", \"경제적 자유 \uD83D\uDCB0\", \"자격증 취득 \uD83C\uDFC5\" ] }]",
  )
  val chat: List<ChatResponseDto>,
  val chatType: ChatType,
) {
  companion object {
    fun from(
      conversation: Conversation,
      assistantResponse: AssistantResponse,
    ): ChatResponse =
      ChatResponse(
        conversationId = conversation.id,
        chat = assistantResponse.responses,
        chatType = ChatType.ASSISTANT,
      )

    fun from(dto: AssistantRequestDto): ChatResponse =
      ChatResponse(
        conversationId = dto.conversationId,
        chat = listOf(ChatResponseDto(AssistantResponseType.TEXT, listOf(dto.text))),
        chatType = ChatType.USER,
      )

    fun from(
      chat: ChatEntity,
      objectMapper: ObjectMapper,
    ): ChatResponse =
      ChatResponse(
        conversationId = chat.conversationId,
        chat =
          objectMapper.readValue(chat.content, List::class.java) as? List<ChatResponseDto>
            ?: throw AssistantError.ChatTransformError(chat.content),
        chatType = ChatType.ASSISTANT,
      )
  }
}
