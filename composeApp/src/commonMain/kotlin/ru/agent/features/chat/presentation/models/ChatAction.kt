package ru.agent.features.chat.presentation.models

sealed class ChatAction {
    object ScrollToBottom : ChatAction()
    data class ShowError(val message: String) : ChatAction()
    data class NavigateToSession(val sessionId: String) : ChatAction()
    object ShowDeleteConfirmation : ChatAction()
}
