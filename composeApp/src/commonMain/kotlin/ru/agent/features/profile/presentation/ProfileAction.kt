package ru.agent.features.profile.presentation

/**
 * Действия экрана профиля (one-time events).
 */
sealed class ProfileAction {
    object ProfileSaved : ProfileAction()
    object DismissDialog : ProfileAction()
    data class ShowError(val message: String) : ProfileAction()
}
