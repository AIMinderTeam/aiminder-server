package ai.aiminder.aiminderserver.assistant.service

import ai.aiminder.aiminderserver.conversation.domain.Conversation
import ai.aiminder.aiminderserver.goal.domain.Goal
import ai.aiminder.aiminderserver.notification.event.CreateFeedbackEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class FeedbackEventService(
  private val publisher: ApplicationEventPublisher,
) {
  suspend fun publish(
    goal: Goal,
    conversation: Conversation,
  ) {
    publisher.publishEvent(CreateFeedbackEvent.from(goal, conversation))
  }
}
