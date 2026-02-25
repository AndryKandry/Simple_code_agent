package ru.agent.features.chat.domain.model

import ru.agent.core.time.currentTimeMillis

data class ChatSession(
    val id: String,
    val title: String = "New Chat",
    val messages: List<Message> = emptyList(),
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val isArchived: Boolean = false
)
