package ai.aiminder.aiminderserver.assistant.domain

import java.time.LocalDate

data class Schedule(
    val date: LocalDate,
    val task: String,
)
