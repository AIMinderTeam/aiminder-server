package ai.aiminder.aiminderserver.controller

import ai.aiminder.aiminderserver.domain.EvaluateGoalResult
import ai.aiminder.aiminderserver.dto.EvaluateGoalRequest
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.withContext
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.entity
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/goal")
class GoalController(
    @Qualifier("goalChatClient")
    private val chatClient: ChatClient,
) {
    @PostMapping
    suspend fun evaluateGoal(
        @RequestBody
        request: EvaluateGoalRequest,
    ): EvaluateGoalResult =
        withContext(Default) {
            chatClient
                .prompt()
                .user(request.text)
                .call()
                .entity<EvaluateGoalResult>()
        }
}
