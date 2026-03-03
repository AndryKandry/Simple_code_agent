package ru.agent.features.profile.domain.usecase

import ru.agent.features.memory.domain.model.UserProfile
import ru.agent.features.profile.domain.repository.UserProfileRepository

/**
 * UseCase для получения профиля пользователя.
 */
class GetUserProfileUseCase(
    private val userProfileRepository: UserProfileRepository
) {
    /**
     * Получить профиль пользователя.
     *
     * @param userId ID пользователя (по умолчанию "default")
     * @return UserProfile или null если не найден
     */
    suspend operator fun invoke(userId: String = UserProfileRepository.DEFAULT_USER_ID): UserProfile? {
        return userProfileRepository.getUserProfile(userId)
    }
}
