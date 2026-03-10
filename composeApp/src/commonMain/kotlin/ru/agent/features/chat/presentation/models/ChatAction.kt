package ru.agent.features.chat.presentation.models

import ru.agent.features.task.domain.validator.TransitionViolation

sealed class ChatAction {
    object ScrollToBottom : ChatAction()
    data class ShowError(val message: String) : ChatAction()
    data class ShowSuccess(val message: String) : ChatAction()
    data class NavigateToSession(val sessionId: String) : ChatAction()
    object ShowDeleteConfirmation : ChatAction()
    /**
     * Show transition warnings to the user.
     * These are non-blocking violations that occurred during task stage transitions.
     */
    data class ShowTransitionWarnings(val warnings: List<TransitionViolation>) : ChatAction()
}
