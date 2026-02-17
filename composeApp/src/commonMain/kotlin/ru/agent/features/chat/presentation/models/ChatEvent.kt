package ru.agent.features.chat.presentation.models

sealed class ChatEvent {
    data class SendMessage(val text: String) : ChatEvent()
    data class InputTextChanged(val text: String) : ChatEvent()
    object ClearError : ChatEvent()
    object ClearHistory : ChatEvent()
}
