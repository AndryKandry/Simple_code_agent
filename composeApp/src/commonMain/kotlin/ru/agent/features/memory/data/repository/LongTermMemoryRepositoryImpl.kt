package ru.agent.features.memory.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.memory.data.local.dao.ContextAnchorDao
import ru.agent.features.memory.data.local.dao.KnowledgeEntryDao
import ru.agent.features.memory.data.local.dao.UserProfileDao
import ru.agent.features.memory.data.local.mapper.MemoryMapper.toDomain
import ru.agent.features.memory.data.local.mapper.MemoryMapper.toEntity
import ru.agent.features.memory.domain.model.AnchorType
import ru.agent.features.memory.domain.model.ContextAnchor
import ru.agent.features.memory.domain.model.KnowledgeCategory
import ru.agent.features.memory.domain.model.KnowledgeEntry
import ru.agent.features.memory.domain.model.UserProfile
import ru.agent.features.memory.domain.repository.LongTermMemoryRepository

/**
 * Repository implementation for Long-term Memory (LTM)
 *
 * Stores user profile, knowledge base, and context anchors in Room Database.
 */
class LongTermMemoryRepositoryImpl(
    private val userProfileDao: UserProfileDao,
    private val knowledgeEntryDao: KnowledgeEntryDao,
    private val contextAnchorDao: ContextAnchorDao
) : LongTermMemoryRepository {

    private val logger = Logger.withTag("LongTermMemoryRepository")

    companion object {
        const val DEFAULT_USER_ID = "default"
    }

    // === User Profile ===

    override suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            withContext(Dispatchers.IO) {
                userProfileDao.getById(userId)?.toDomain()
            }
        } catch (e: Exception) {
            logger.e { "Error getting user profile: ${e.message}" }
            null
        }
    }

    override suspend fun saveUserProfile(profile: UserProfile): ResultWrapper<UserProfile> {
        return try {
            withContext(Dispatchers.IO) {
                val entity = profile.toEntity()
                userProfileDao.insert(entity)
                logger.i { "User profile saved: ${profile.id}" }
                ResultWrapper.Success(profile)
            }
        } catch (e: Exception) {
            logger.e { "Error saving user profile: ${e.message}" }
            ResultWrapper.Error(throwable = e, message = "Failed to save user profile")
        }
    }

    override suspend fun getOrCreateDefaultProfile(): UserProfile {
        return try {
            withContext(Dispatchers.IO) {
                val existing = userProfileDao.getDefaultProfile()
                if (existing != null) {
                    existing.toDomain()
                } else {
                    val now = currentTimeMillis()
                    val profile = UserProfile(
                        id = DEFAULT_USER_ID,
                        name = "User",
                        createdAt = now,
                        updatedAt = now
                    )
                    val entity = profile.toEntity()
                    userProfileDao.insert(entity)
                    logger.i { "Created default user profile" }
                    profile
                }
            }
        } catch (e: Exception) {
            logger.e { "Error creating default profile: ${e.message}" }
            UserProfile(
                id = DEFAULT_USER_ID,
                name = "User",
                createdAt = currentTimeMillis(),
                updatedAt = currentTimeMillis()
            )
        }
    }

    // === Knowledge Base ===

    override suspend fun getKnowledgeEntry(key: String): KnowledgeEntry? {
        return try {
            withContext(Dispatchers.IO) {
                knowledgeEntryDao.getByKey(key)?.toDomain()
            }
        } catch (e: Exception) {
            logger.e { "Error getting knowledge entry: ${e.message}" }
            null
        }
    }

    override suspend fun getKnowledgeEntryById(id: String): KnowledgeEntry? {
        return try {
            withContext(Dispatchers.IO) {
                knowledgeEntryDao.getById(id)?.toDomain()
            }
        } catch (e: Exception) {
            logger.e { "Error getting knowledge entry by id: ${e.message}" }
            null
        }
    }

    override suspend fun saveKnowledgeEntry(entry: KnowledgeEntry): ResultWrapper<KnowledgeEntry> {
        return try {
            withContext(Dispatchers.IO) {
                val entity = entry.toEntity()
                knowledgeEntryDao.insert(entity)
                logger.i { "Knowledge entry saved: ${entry.id}" }
                ResultWrapper.Success(entry)
            }
        } catch (e: Exception) {
            logger.e { "Error saving knowledge entry: ${e.message}" }
            ResultWrapper.Error(throwable = e, message = "Failed to save knowledge entry")
        }
    }

    override suspend fun deleteKnowledgeEntry(id: String) {
        try {
            withContext(Dispatchers.IO) {
                knowledgeEntryDao.deleteById(id)
                logger.i { "Knowledge entry deleted: $id" }
            }
        } catch (e: Exception) {
            logger.e { "Error deleting knowledge entry: ${e.message}" }
        }
    }

    override suspend fun searchKnowledge(
        query: String,
        category: KnowledgeCategory?,
        limit: Int
    ): List<KnowledgeEntry> {
        return try {
            withContext(Dispatchers.IO) {
                val now = currentTimeMillis()
                val results = if (category != null) {
                    knowledgeEntryDao.searchByCategory(query, category.name, now, limit)
                } else {
                    knowledgeEntryDao.search(query, now, limit)
                }
                results.map { it.toDomain() }
            }
        } catch (e: Exception) {
            logger.e { "Error searching knowledge: ${e.message}" }
            emptyList()
        }
    }

    override suspend fun getEntriesByTags(tags: List<String>, limit: Int): List<KnowledgeEntry> {
        return try {
            withContext(Dispatchers.IO) {
                val all = knowledgeEntryDao.getAll()
                val now = currentTimeMillis()
                val filtered = all
                    .filter { entry ->
                        val entryTags = entry.tags?.split(",")?.map { it.trim() } ?: emptyList()
                        tags.any { tag -> entryTags.contains(tag) }
                    }
                    .filter { it.expiresAt == null || it.expiresAt > now }
                    .sortedByDescending { it.relevanceScore }
                    .take(limit)
                    .map { it.toDomain() }
                filtered
            }
        } catch (e: Exception) {
            logger.e { "Error getting entries by tags: ${e.message}" }
            emptyList()
        }
    }

    override suspend fun getEntriesByCategory(category: KnowledgeCategory, limit: Int): List<KnowledgeEntry> {
        return try {
            withContext(Dispatchers.IO) {
                val now = currentTimeMillis()
                knowledgeEntryDao.getByCategory(category.name, now, limit)
                    .map { it.toDomain() }
            }
        } catch (e: Exception) {
            logger.e { "Error getting entries by category: ${e.message}" }
            emptyList()
        }
    }

    override suspend fun incrementAccessCount(id: String) {
        try {
            withContext(Dispatchers.IO) {
                knowledgeEntryDao.incrementAccessCount(id, currentTimeMillis())
                logger.d { "Access count incremented for: $id" }
            }
        } catch (e: Exception) {
            logger.e { "Error incrementing access count: ${e.message}" }
        }
    }

    override fun getAllKnowledgeFlow(): Flow<List<KnowledgeEntry>> {
        return knowledgeEntryDao.getAllFlow()
            .map { entities -> entities.map { it.toDomain() } }
    }

    // === Context Anchors ===

    override suspend fun getAnchor(id: String): ContextAnchor? {
        return try {
            withContext(Dispatchers.IO) {
                contextAnchorDao.getById(id)?.toDomain()
            }
        } catch (e: Exception) {
            logger.e { "Error getting anchor: ${e.message}" }
            null
        }
    }

    override suspend fun saveAnchor(anchor: ContextAnchor): ResultWrapper<ContextAnchor> {
        return try {
            withContext(Dispatchers.IO) {
                val entity = anchor.toEntity()
                contextAnchorDao.insert(entity)
                logger.i { "Anchor saved: ${anchor.id}" }
                ResultWrapper.Success(anchor)
            }
        } catch (e: Exception) {
            logger.e { "Error saving anchor: ${e.message}" }
            ResultWrapper.Error(throwable = e, message = "Failed to save anchor")
        }
    }

    override suspend fun deleteAnchor(id: String) {
        try {
            withContext(Dispatchers.IO) {
                contextAnchorDao.deleteById(id)
                logger.i { "Anchor deleted: $id" }
            }
        } catch (e: Exception) {
            logger.e { "Error deleting anchor: ${e.message}" }
        }
    }

    override suspend fun getActiveAnchors(): List<ContextAnchor> {
        return try {
            withContext(Dispatchers.IO) {
                contextAnchorDao.getActiveAnchors().map { it.toDomain() }
            }
        } catch (e: Exception) {
            logger.e { "Error getting active anchors: ${e.message}" }
            emptyList()
        }
    }

    override suspend fun getAnchorsByType(type: AnchorType): List<ContextAnchor> {
        return try {
            withContext(Dispatchers.IO) {
                contextAnchorDao.getByType(type.name).map { it.toDomain() }
            }
        } catch (e: Exception) {
            logger.e { "Error getting anchors by type: ${e.message}" }
            emptyList()
        }
    }

    override suspend fun deactivateAnchor(id: String) {
        try {
            withContext(Dispatchers.IO) {
                contextAnchorDao.deactivate(id, currentTimeMillis())
                logger.i { "Anchor deactivated: $id" }
            }
        } catch (e: Exception) {
            logger.e { "Error deactivating anchor: ${e.message}" }
        }
    }

    override suspend fun activateAnchor(id: String) {
        try {
            withContext(Dispatchers.IO) {
                contextAnchorDao.activate(id, currentTimeMillis())
                logger.i { "Anchor activated: $id" }
            }
        } catch (e: Exception) {
            logger.e { "Error activating anchor: ${e.message}" }
        }
    }

    override suspend fun touchAnchor(id: String) {
        try {
            withContext(Dispatchers.IO) {
                contextAnchorDao.touch(id, currentTimeMillis())
                logger.d { "Anchor touched: $id" }
            }
        } catch (e: Exception) {
            logger.e { "Error touching anchor: ${e.message}" }
        }
    }

    override fun getActiveAnchorsFlow(): Flow<List<ContextAnchor>> {
        return contextAnchorDao.getActiveAnchorsFlow()
            .map { entities -> entities.map { it.toDomain() } }
    }
}
