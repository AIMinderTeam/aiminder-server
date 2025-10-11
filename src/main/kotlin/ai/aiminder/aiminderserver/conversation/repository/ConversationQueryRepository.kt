package ai.aiminder.aiminderserver.conversation.repository

import ai.aiminder.aiminderserver.assistant.domain.ChatRow
import ai.aiminder.aiminderserver.common.config.JooqR2dbcRepository
import ai.aiminder.aiminderserver.conversation.dto.GetConversationRequestDto
import ai.aiminder.aiminderserver.conversation.dto.GetMessagesRequestDto
import ai.aiminder.aiminderserver.conversation.repository.row.ConversationRow
import ai.aiminder.aiminderserver.jooq.tables.Conversations.Companion.CONVERSATIONS
import ai.aiminder.aiminderserver.jooq.tables.Goals.Companion.GOALS
import ai.aiminder.aiminderserver.jooq.tables.SpringAiChatMemory.Companion.SPRING_AI_CHAT_MEMORY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import org.jooq.Condition
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ConversationQueryRepository : JooqR2dbcRepository() {
  suspend fun findConversationsBy(dto: GetConversationRequestDto): Flow<ConversationRow> =
    query {
      select(
        CONVERSATIONS.CONVERSATION_ID,
        DSL
          .field(
            "({0})",
            String::class.java,
            DSL
              .select(SPRING_AI_CHAT_MEMORY.CONTENT)
              .from(SPRING_AI_CHAT_MEMORY)
              .where(SPRING_AI_CHAT_MEMORY.CONVERSATION_ID.eq(CONVERSATIONS.CONVERSATION_ID.cast(String::class.java)))
              .orderBy(SPRING_AI_CHAT_MEMORY.TIMESTAMP.desc())
              .limit(1),
          ).`as`("recent_chat"),
        DSL
          .field(
            "({0})",
            LocalDateTime::class.java,
            DSL
              .select(SPRING_AI_CHAT_MEMORY.TIMESTAMP)
              .from(SPRING_AI_CHAT_MEMORY)
              .where(SPRING_AI_CHAT_MEMORY.CONVERSATION_ID.eq(CONVERSATIONS.CONVERSATION_ID.cast(String::class.java)))
              .orderBy(SPRING_AI_CHAT_MEMORY.TIMESTAMP.desc())
              .limit(1),
          ).`as`("recent_at"),
        DSL
          .field(
            "({0})",
            String::class.java,
            DSL
              .select(SPRING_AI_CHAT_MEMORY.TYPE)
              .from(SPRING_AI_CHAT_MEMORY)
              .where(SPRING_AI_CHAT_MEMORY.CONVERSATION_ID.eq(CONVERSATIONS.CONVERSATION_ID.cast(String::class.java)))
              .orderBy(SPRING_AI_CHAT_MEMORY.TIMESTAMP.desc())
              .limit(1),
          ).`as`("type"),
        CONVERSATIONS.GOAL_ID,
        GOALS.TITLE.`as`("goal_title"),
      ).from(CONVERSATIONS)
        .leftJoin(GOALS)
        .on(CONVERSATIONS.GOAL_ID.eq(GOALS.GOAL_ID).and(GOALS.DELETED_AT.isNull))
        .where(buildConversationConditions(dto))
        .orderBy(DSL.field("recent_at").desc().nullsLast())
        .offset(dto.pageable.offset.toInt())
        .limit(dto.pageable.pageSize)
    }.map { record ->
      ConversationRow(
        conversationId = record.get(CONVERSATIONS.CONVERSATION_ID)!!,
        recentChat = record.get("recent_chat", String::class.java) ?: "",
        type = record.get("type", String::class.java),
        recentAt = record.get("recent_at", LocalDateTime::class.java),
        goalId = record.get(CONVERSATIONS.GOAL_ID),
        goalTitle = record.get("goal_title", String::class.java),
      )
    }

  suspend fun findChatBy(dto: GetMessagesRequestDto): Flow<ChatRow> =
    query {
      select(
        SPRING_AI_CHAT_MEMORY.CONTENT,
        SPRING_AI_CHAT_MEMORY.TYPE,
      ).from(SPRING_AI_CHAT_MEMORY)
        .where(SPRING_AI_CHAT_MEMORY.CONVERSATION_ID.eq(dto.conversationId.toString()))
        .orderBy(SPRING_AI_CHAT_MEMORY.TIMESTAMP.desc())
        .offset(dto.pageable.offset.toInt())
        .limit(dto.pageable.pageSize)
    }.map { record ->
      ChatRow(
        content = record.get(SPRING_AI_CHAT_MEMORY.CONTENT)!!,
        type = record.get(SPRING_AI_CHAT_MEMORY.TYPE)!!,
      )
    }

  suspend fun countBy(dto: GetConversationRequestDto): Long =
    query {
      selectCount()
        .from(CONVERSATIONS)
        .where(buildConversationConditions(dto))
    }.single().component1().toLong()

  suspend fun countBy(dto: GetMessagesRequestDto): Long =
    query {
      selectCount()
        .from(SPRING_AI_CHAT_MEMORY)
        .where(SPRING_AI_CHAT_MEMORY.CONVERSATION_ID.eq(dto.conversationId.toString()))
    }.single().component1().toLong()

  private fun buildConversationConditions(dto: GetConversationRequestDto): List<Condition> {
    val conditions = mutableListOf<Condition>()

    conditions.add(CONVERSATIONS.DELETED_AT.isNull)
    conditions.add(CONVERSATIONS.USER_ID.eq(dto.userId))

    return conditions
  }
}
