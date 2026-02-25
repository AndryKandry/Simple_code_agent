package ru.agent.features.chat.presentation.models

sealed class ChatEvent {
    data class SendMessage(val text: String) : ChatEvent()
    data class InputTextChanged(val text: String) : ChatEvent()
    data class SelectSession(val sessionId: String) : ChatEvent()
    object CreateNewSession : ChatEvent()
    data class DeleteSession(val sessionId: String) : ChatEvent()
    object ClearError : ChatEvent()
    data class ClearHistory(val sessionId: String) : ChatEvent()
    object ToggleSidebar : ChatEvent()
    data class LoadSession(val sessionId: String) : ChatEvent()
}
