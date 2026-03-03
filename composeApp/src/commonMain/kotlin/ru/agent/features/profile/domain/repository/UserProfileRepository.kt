package ru.agent.features.profile.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.features.memory.domain.model.UserProfile

/**
 * Repository для управления профилем пользователя.
 */
interface UserProfileRepository {

    /**
     * Получить профиль пользователя по ID.
     *
     * @param userId ID пользователя (по умолчанию "default")
     * @return UserProfile или null если не найден
     */
    suspend fun getUserProfile(userId: String = DEFAULT_USER_ID): UserProfile?

    /**
     * Получить профиль пользователя как Flow.
     *
     * @param userId ID пользователя
     * @return Flow с UserProfile или null
     */
    fun getUserProfileFlow(userId: String = DEFAULT_USER_ID): Flow<UserProfile?>

    /**
     * Обновить профиль пользователя.
     *
     * @param profile Обновленный профиль
     */
    suspend fun updateUserProfile(profile: UserProfile)

    /**
     * Создать профиль по умолчанию.
     *
     * @return Созданный профиль
     */
    suspend fun createDefaultProfile(): UserProfile

    /**
     * Проверить существование профиля.
     *
     * @param userId ID пользователя
     * @return true если профиль существует
     */
    suspend fun profileExists(userId: String = DEFAULT_USER_ID): Boolean

    companion object {
        const val DEFAULT_USER_ID = "default"
    }
}
