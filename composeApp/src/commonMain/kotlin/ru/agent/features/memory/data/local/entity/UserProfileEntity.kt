package ru.agent.features.memory.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity для профиля пользователя (User Profile).
 *
 * Хранит настройки и предпочтения пользователя.
 */
@Entity(
    tableName = "user_profiles",
    indices = [
        Index(value = ["name"])
    ]
)
data class UserProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    // Preferences as JSON
    val preferredLanguage: String,
    val codeStyleIndentSize: Int,
    val codeStyleUseTabs: Boolean,
    val codeStyleMaxLineLength: Int,
    val codeStyleTrailingComma: Boolean,
    val theme: String,
    val responseVerbosity: String,
    // Custom instructions as JSON array string
    val customInstructions: String?,
    // Interaction stats as JSON
    val totalMessages: Long,
    val totalSessions: Long,
    val totalTasksCompleted: Long,
    val averageSessionLength: Float,
    // Most used task types as JSON map string
    val mostUsedTaskTypes: String?,
    val lastActiveAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
