package ru.agent.features.profile.presentation

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import ru.agent.core.presentation.BaseViewModel
import ru.agent.features.profile.domain.usecase.CreateDefaultProfileUseCase
import ru.agent.features.profile.domain.usecase.GetUserProfileUseCase
import ru.agent.features.profile.domain.usecase.UpdateUserProfileUseCase

/**
 * ViewModel для управления профилем пользователя.
 */
class ProfileViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val createDefaultProfileUseCase: CreateDefaultProfileUseCase
) : BaseViewModel<ProfileState, ProfileAction, ProfileEvent>(
    initialState = ProfileState(isLoading = true)
) {

    private val logger = Logger.withTag("ProfileViewModel")

    init {
        loadProfile()
    }

    override fun obtainEvent(viewEvent: ProfileEvent) {
        when (viewEvent) {
            is ProfileEvent.LoadProfile -> loadProfile()
            is ProfileEvent.SaveProfile -> saveProfile()
            is ProfileEvent.ClearError -> viewState = viewState.copy(error = null)
            is ProfileEvent.ClearSaved -> viewState = viewState.copy(isSaved = false)
            is ProfileEvent.ResetToDefaults -> resetToDefaults()

            // Avatar
            is ProfileEvent.AvatarColorChanged -> updateAvatarColor(viewEvent.colorIndex)

            // Form fields
            is ProfileEvent.NameChanged -> updateName(viewEvent.name)
            is ProfileEvent.RoleChanged -> updateRole(viewEvent.role)
            is ProfileEvent.ContextChanged -> updateContext(viewEvent.context)
            is ProfileEvent.LanguageChanged -> updateLanguage(viewEvent.language)
            is ProfileEvent.VerbosityChanged -> updateVerbosity(viewEvent.verbosity)
            is ProfileEvent.IndentSizeChanged -> updateIndentSize(viewEvent.size)
            is ProfileEvent.UseTabsChanged -> updateUseTabs(viewEvent.useTabs)
            is ProfileEvent.MaxLineLengthChanged -> updateMaxLineLength(viewEvent.length)
            is ProfileEvent.TrailingCommaChanged -> updateTrailingComma(viewEvent.enabled)
            is ProfileEvent.CustomInstructionsChanged -> updateCustomInstructions(viewEvent.instructions)
        }
    }

    private fun loadProfile() {
        logger.i { "Loading user profile" }
        viewState = viewState.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Сначала пытаемся создать default профиль если его нет
                val profile = createDefaultProfileUseCase()
                logger.i { "Profile loaded: ${profile.id}" }
                viewState = viewState.copy(
                    profile = profile,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to load profile" }
                viewState = viewState.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load profile"
                )
                viewAction = ProfileAction.ShowError(e.message ?: "Failed to load profile")
            }
        }
    }

    private fun saveProfile() {
        val currentProfile = viewState.profile
        if (currentProfile == null) {
            logger.w { "Cannot save: profile is null" }
            return
        }

        logger.i { "Saving profile" }
        viewState = viewState.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                updateUserProfileUseCase(currentProfile)
                logger.i { "Profile saved successfully" }
                viewState = viewState.copy(
                    isSaving = false,
                    isSaved = true
                )
                viewAction = ProfileAction.ProfileSaved
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to save profile" }
                viewState = viewState.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save profile"
                )
                viewAction = ProfileAction.ShowError(e.message ?: "Failed to save profile")
            }
        }
    }

    private fun resetToDefaults() {
        val currentProfile = viewState.profile
        if (currentProfile == null) {
            logger.w { "Cannot reset: profile is null" }
            return
        }

        logger.i { "Resetting profile to defaults" }

        val defaultProfile = ProfileState.defaultProfile(currentProfile.id)

        viewState = viewState.copy(
            profile = defaultProfile,
            avatarColorIndex = 0
        )

        logger.i { "Profile reset to defaults" }
    }

    // === Form field updates ===

    private fun updateAvatarColor(colorIndex: Int) {
        viewState = viewState.copy(avatarColorIndex = colorIndex.coerceIn(0, 7))
    }

    private fun updateName(name: String) {
        val currentProfile = viewState.profile ?: return
        val trimmedName = name.trim()
        viewState = viewState.copy(
            profile = currentProfile.copy(name = trimmedName)
        )
    }

    private fun updateRole(role: String) {
        val currentProfile = viewState.profile ?: return
        viewState = viewState.copy(
            profile = currentProfile.copy(role = role)
        )
    }

    private fun updateContext(context: String) {
        val currentProfile = viewState.profile ?: return
        viewState = viewState.copy(
            profile = currentProfile.copy(context = context)
        )
    }

    private fun updateLanguage(language: String) {
        val currentProfile = viewState.profile ?: return
        viewState = viewState.copy(
            profile = currentProfile.copy(
                preferences = currentProfile.preferences.copy(
                    preferredLanguage = language
                )
            )
        )
    }

    private fun updateVerbosity(verbosity: ru.agent.features.memory.domain.model.ResponseVerbosity) {
        val currentProfile = viewState.profile ?: return
        viewState = viewState.copy(
            profile = currentProfile.copy(
                preferences = currentProfile.preferences.copy(
                    responseVerbosity = verbosity
                )
            )
        )
    }

    private fun updateIndentSize(size: Int) {
        val currentProfile = viewState.profile ?: return
        // Валидация: indentSize должен быть в диапазоне 1-16
        val validatedSize = size.coerceIn(1, 16)
        viewState = viewState.copy(
            profile = currentProfile.copy(
                preferences = currentProfile.preferences.copy(
                    codeStyle = currentProfile.preferences.codeStyle.copy(
                        indentSize = validatedSize
                    )
                )
            )
        )
    }

    private fun updateUseTabs(useTabs: Boolean) {
        val currentProfile = viewState.profile ?: return
        viewState = viewState.copy(
            profile = currentProfile.copy(
                preferences = currentProfile.preferences.copy(
                    codeStyle = currentProfile.preferences.codeStyle.copy(
                        useTabs = useTabs
                    )
                )
            )
        )
    }

    private fun updateMaxLineLength(length: Int) {
        val currentProfile = viewState.profile ?: return
        // Валидация: maxLineLength должен быть в диапазоне 40-200
        val validatedLength = length.coerceIn(40, 200)
        viewState = viewState.copy(
            profile = currentProfile.copy(
                preferences = currentProfile.preferences.copy(
                    codeStyle = currentProfile.preferences.codeStyle.copy(
                        maxLineLength = validatedLength
                    )
                )
            )
        )
    }

    private fun updateTrailingComma(enabled: Boolean) {
        val currentProfile = viewState.profile ?: return
        viewState = viewState.copy(
            profile = currentProfile.copy(
                preferences = currentProfile.preferences.copy(
                    codeStyle = currentProfile.preferences.codeStyle.copy(
                        trailingComma = enabled
                    )
                )
            )
        )
    }

    private fun updateCustomInstructions(instructions: List<String>) {
        val currentProfile = viewState.profile ?: return
        viewState = viewState.copy(
            profile = currentProfile.copy(
                preferences = currentProfile.preferences.copy(
                    customInstructions = instructions
                )
            )
        )
    }
}
