package ru.agent.features.profile.presentation

import ru.agent.core.time.currentTimeMillis
import ru.agent.features.memory.domain.model.CodeStyle
import ru.agent.features.memory.domain.model.ResponseVerbosity
import ru.agent.features.memory.domain.model.UserProfile
import ru.agent.features.profile.presentation.components.formatNumber
import ru.agent.features.profile.presentation.components.isRecentlyActive

/**
 * Состояние экрана профиля.
 */
data class ProfileState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val avatarColorIndex: Int = 0
) {
    // Form fields
    val name: String get() = profile?.name ?: "User"
    val role: String get() = profile?.role ?: "developer"
    val context: String get() = profile?.context ?: ""
    val preferredLanguage: String get() = profile?.preferences?.preferredLanguage ?: "kotlin"
    val codeStyle: CodeStyle get() = profile?.preferences?.codeStyle ?: CodeStyle()
    val responseVerbosity: ResponseVerbosity get() = profile?.preferences?.responseVerbosity ?: ResponseVerbosity.NORMAL
    val customInstructions: List<String> get() = profile?.preferences?.customInstructions ?: emptyList()

    // Code style fields
    val indentSize: Int get() = codeStyle.indentSize
    val useTabs: Boolean get() = codeStyle.useTabs
    val maxLineLength: Int get() = codeStyle.maxLineLength
    val trailingComma: Boolean get() = codeStyle.trailingComma

    // Stats fields (formatted for display)
    val formattedTotalMessages: String
        get() = formatNumber(profile?.interactionStats?.totalMessages ?: 0)

    val formattedTotalSessions: String
        get() = formatNumber(profile?.interactionStats?.totalSessions ?: 0)

    val formattedTotalTasks: String
        get() = formatNumber(profile?.interactionStats?.totalTasksCompleted ?: 0)

    val lastActiveAt: Long?
        get() = profile?.interactionStats?.lastActiveAt

    val isActiveRecently: Boolean
        get() = isRecentlyActive(profile?.interactionStats?.lastActiveAt)

    val formattedLastActive: String
        get() = formatLastActive(profile?.interactionStats?.lastActiveAt)

    /**
     * Форматирование времени последней активности.
     */
    private fun formatLastActive(timestamp: Long?): String {
        if (timestamp == null) return "Never active"
        val now = currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Active now"
            diff < 3600_000 -> "Active ${diff / 60_000}m ago"
            diff < 86400_000 -> "Active ${diff / 3600_000}h ago"
            diff < 604800_000 -> "Active ${diff / 86400_000}d ago"
            else -> "Active long ago"
        }
    }

    companion object {
        /**
         * Дефолтные значения профиля.
         */
        fun defaultProfile(id: String): UserProfile = UserProfile(
            id = id,
            name = "User",
            role = "developer",
            context = "",
            preferences = ru.agent.features.memory.domain.model.UserPreferences(
                preferredLanguage = "kotlin",
                codeStyle = CodeStyle(
                    indentSize = 4,
                    useTabs = false,
                    maxLineLength = 120,
                    trailingComma = true
                ),
                theme = "dark",
                responseVerbosity = ResponseVerbosity.NORMAL,
                customInstructions = emptyList()
            ),
            interactionStats = ru.agent.features.memory.domain.model.InteractionStats(),
            createdAt = currentTimeMillis(),
            updatedAt = currentTimeMillis()
        )
    }
}
