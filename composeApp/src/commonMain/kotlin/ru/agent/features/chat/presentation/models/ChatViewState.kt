package ru.agent.features.chat.presentation.models

import ru.agent.features.chat.domain.model.Message

data class ChatViewState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = ""
)
