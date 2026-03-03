package ru.agent.features.profile.domain.usecase

import ru.agent.features.memory.domain.model.UserProfile
import ru.agent.features.profile.domain.repository.UserProfileRepository

/**
 * UseCase для создания профиля по умолчанию.
 *
 * Создает профиль с дефолтными значениями, если профиль еще не существует.
 */
class CreateDefaultProfileUseCase(
    private val userProfileRepository: UserProfileRepository
) {
    /**
     * Создать профиль по умолчанию.
     *
     * @return Созданный или существующий профиль
     */
    suspend operator fun invoke(): UserProfile {
        // Проверяем, существует ли уже профиль
        val existingProfile = userProfileRepository.getUserProfile()
        if (existingProfile != null) {
            return existingProfile
        }

        // Создаем новый профиль
        return userProfileRepository.createDefaultProfile()
    }
}
