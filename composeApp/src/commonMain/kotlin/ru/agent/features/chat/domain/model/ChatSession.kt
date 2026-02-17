package ru.agent.features.chat.domain.model

import ru.agent.core.time.currentTimeMillis

data class ChatSession(
    val id: String,
    val messages: List<Message> = emptyList(),
    val createdAt: Long = currentTimeMillis()
)
