package ru.agent.features.chat.domain.model

data class Message(
    val id: String,
    val content: String,
    val senderType: SenderType,
    val timestamp: Long
)
