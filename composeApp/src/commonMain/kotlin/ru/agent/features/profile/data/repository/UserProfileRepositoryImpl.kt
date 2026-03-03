package ru.agent.features.profile.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.memory.data.local.dao.UserProfileDao
import ru.agent.features.memory.data.local.mapper.MemoryMapper.toDomain
import ru.agent.features.memory.data.local.mapper.MemoryMapper.toEntity
import ru.agent.features.memory.domain.model.InteractionStats
import ru.agent.features.memory.domain.model.UserPreferences
import ru.agent.features.memory.domain.model.UserProfile
import ru.agent.features.profile.domain.repository.UserProfileRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Реализация UserProfileRepository.
 */
class UserProfileRepositoryImpl(
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {

    private val logger = Logger.withTag("UserProfileRepository")

    override suspend fun getUserProfile(userId: String): UserProfile? {
        logger.d { "Getting user profile: $userId" }
        val entity = userProfileDao.getById(userId)
        return entity?.toDomain()
    }

    override fun getUserProfileFlow(userId: String): Flow<UserProfile?> {
        logger.d { "Getting user profile flow: $userId" }
        return userProfileDao.getByIdFlow(userId).map { entity ->
            entity?.toDomain()
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun updateUserProfile(profile: UserProfile) {
        logger.i { "Updating user profile: ${profile.id}" }
        val entity = profile.toEntity()
        userProfileDao.insert(entity)
        logger.i { "Profile updated successfully" }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun createDefaultProfile(): UserProfile {
        logger.i { "Creating default user profile" }

        val now = currentTimeMillis()
        val profile = UserProfile(
            id = UserProfileRepository.DEFAULT_USER_ID,
            name = "User",
            role = "developer",
            context = "",
            preferences = UserPreferences(
                preferredLanguage = "kotlin",
                theme = "dark",
                responseVerbosity = ru.agent.features.memory.domain.model.ResponseVerbosity.NORMAL,
                customInstructions = emptyList()
            ),
            interactionStats = InteractionStats(
                totalMessages = 0,
                totalSessions = 0,
                totalTasksCompleted = 0,
                averageSessionLength = 0f,
                mostUsedTaskTypes = emptyMap(),
                lastActiveAt = now
            ),
            createdAt = now,
            updatedAt = now
        )

        userProfileDao.insert(profile.toEntity())
        logger.i { "Default profile created with ID: ${profile.id}" }

        return profile
    }

    override suspend fun profileExists(userId: String): Boolean {
        return userProfileDao.exists(userId)
    }
}
