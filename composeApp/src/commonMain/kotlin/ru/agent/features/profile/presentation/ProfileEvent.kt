package ru.agent.features.profile.presentation

import ru.agent.features.memory.domain.model.ResponseVerbosity

/**
 * События экрана профиля.
 */
sealed class ProfileEvent {
    // Lifecycle
    object LoadProfile : ProfileEvent()
    object SaveProfile : ProfileEvent()
    object ClearError : ProfileEvent()
    object ClearSaved : ProfileEvent()
    object ResetToDefaults : ProfileEvent()

    // Avatar
    data class AvatarColorChanged(val colorIndex: Int) : ProfileEvent()

    // Form fields
    data class NameChanged(val name: String) : ProfileEvent()
    data class RoleChanged(val role: String) : ProfileEvent()
    data class ContextChanged(val context: String) : ProfileEvent()
    data class LanguageChanged(val language: String) : ProfileEvent()
    data class VerbosityChanged(val verbosity: ResponseVerbosity) : ProfileEvent()
    data class IndentSizeChanged(val size: Int) : ProfileEvent()
    data class UseTabsChanged(val useTabs: Boolean) : ProfileEvent()
    data class MaxLineLengthChanged(val length: Int) : ProfileEvent()
    data class TrailingCommaChanged(val enabled: Boolean) : ProfileEvent()
    data class CustomInstructionsChanged(val instructions: List<String>) : ProfileEvent()
}
