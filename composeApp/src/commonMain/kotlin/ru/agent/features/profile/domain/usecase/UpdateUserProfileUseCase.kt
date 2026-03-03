package ru.agent.features.profile.domain.usecase

import ru.agent.core.time.currentTimeMillis
import ru.agent.features.memory.domain.model.UserProfile
import ru.agent.features.profile.domain.repository.UserProfileRepository

/**
 * UseCase для обновления профиля пользователя.
 */
class UpdateUserProfileUseCase(
    private val userProfileRepository: UserProfileRepository
) {
    /**
     * Обновить профиль пользователя.
     *
     * @param profile Профиль с обновленными данными
     */
    suspend operator fun invoke(profile: UserProfile) {
        val updatedProfile = profile.copy(
            updatedAt = currentTimeMillis()
        )
        userProfileRepository.updateUserProfile(updatedProfile)
    }
}
