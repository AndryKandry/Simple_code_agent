package ru.agent.features.memory.data.local.mapper

import ru.agent.features.memory.data.local.entity.ContextAnchorEntity
import ru.agent.features.memory.data.local.entity.KnowledgeEntryEntity
import ru.agent.features.memory.data.local.entity.UserProfileEntity
import ru.agent.features.memory.data.local.entity.WorkingMemoryEntity
import ru.agent.features.memory.domain.model.AnchorType
import ru.agent.features.memory.domain.model.CodeStyle
import ru.agent.features.memory.domain.model.ContextAnchor
import ru.agent.features.memory.domain.model.ExecutionState
import ru.agent.features.memory.domain.model.InteractionStats
import ru.agent.features.memory.domain.model.KnowledgeCategory
import ru.agent.features.memory.domain.model.KnowledgeEntry
import ru.agent.features.memory.domain.model.ResponseVerbosity
import ru.agent.features.memory.domain.model.TaskInfo
import ru.agent.features.memory.domain.model.TaskStatus
import ru.agent.features.memory.domain.model.TaskType
import ru.agent.features.memory.domain.model.UserPreferences
import ru.agent.features.memory.domain.model.UserProfile
import ru.agent.features.memory.domain.model.WorkingMemory

/**
 * Mapper для преобразования между Entity (data layer) и Domain моделями.
 */
object MemoryMapper {

    // === Working Memory ===

    fun WorkingMemoryEntity.toDomain(): WorkingMemory {
        return WorkingMemory(
            id = id,
            sessionId = sessionId,
            taskInfo = toTaskInfo(),
            executionState = ExecutionState.valueOf(executionState),
            temporaryData = temporaryData,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun WorkingMemoryEntity.toTaskInfo(): TaskInfo? {
        if (taskId == null) return null
        return TaskInfo(
            taskId = taskId,
            taskType = taskType?.let { TaskType.valueOf(it) } ?: TaskType.OTHER,
            description = taskDescription ?: "",
            status = taskStatus?.let { TaskStatus.valueOf(it) } ?: TaskStatus.PENDING,
            progress = taskProgress ?: 0f,
            startedAt = taskStartedAt,
            completedAt = taskCompletedAt,
            parentTaskId = parentTaskId
        )
    }

    fun WorkingMemory.toEntity(): WorkingMemoryEntity {
        return WorkingMemoryEntity(
            id = id,
            sessionId = sessionId,
            taskId = taskInfo?.taskId,
            taskType = taskInfo?.taskType?.name,
            taskDescription = taskInfo?.description,
            taskStatus = taskInfo?.status?.name,
            taskProgress = taskInfo?.progress,
            taskStartedAt = taskInfo?.startedAt,
            taskCompletedAt = taskInfo?.completedAt,
            parentTaskId = taskInfo?.parentTaskId,
            executionState = executionState.name,
            temporaryData = temporaryData,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // === Knowledge Entry ===

    fun KnowledgeEntryEntity.toDomain(): KnowledgeEntry {
        return KnowledgeEntry(
            id = id,
            key = key,
            value = value,
            category = KnowledgeCategory.valueOf(category),
            tags = tags?.split(",")?.map { it.trim() } ?: emptyList(),
            relevanceScore = relevanceScore,
            accessCount = accessCount,
            lastAccessedAt = lastAccessedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            expiresAt = expiresAt
        )
    }

    fun KnowledgeEntry.toEntity(): KnowledgeEntryEntity {
        return KnowledgeEntryEntity(
            id = id,
            key = key,
            value = value,
            category = category.name,
            tags = if (tags.isNotEmpty()) tags.joinToString(",") else null,
            relevanceScore = relevanceScore,
            accessCount = accessCount,
            lastAccessedAt = lastAccessedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            expiresAt = expiresAt
        )
    }

    fun List<KnowledgeEntryEntity>.toKnowledgeDomain(): List<KnowledgeEntry> {
        return this.map { it.toDomain() }
    }

    // === User Profile ===

    fun UserProfileEntity.toDomain(): UserProfile {
        return UserProfile(
            id = id,
            name = name,
            preferences = UserPreferences(
                preferredLanguage = preferredLanguage,
                codeStyle = CodeStyle(
                    indentSize = codeStyleIndentSize,
                    useTabs = codeStyleUseTabs,
                    maxLineLength = codeStyleMaxLineLength,
                    trailingComma = codeStyleTrailingComma
                ),
                theme = theme,
                responseVerbosity = ResponseVerbosity.valueOf(responseVerbosity),
                customInstructions = customInstructions?.split("|||") ?: emptyList()
            ),
            interactionStats = InteractionStats(
                totalMessages = totalMessages,
                totalSessions = totalSessions,
                totalTasksCompleted = totalTasksCompleted,
                averageSessionLength = averageSessionLength,
                mostUsedTaskTypes = parseTaskTypesMap(mostUsedTaskTypes),
                lastActiveAt = lastActiveAt
            ),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun UserProfile.toEntity(): UserProfileEntity {
        return UserProfileEntity(
            id = id,
            name = name,
            preferredLanguage = preferences.preferredLanguage,
            codeStyleIndentSize = preferences.codeStyle.indentSize,
            codeStyleUseTabs = preferences.codeStyle.useTabs,
            codeStyleMaxLineLength = preferences.codeStyle.maxLineLength,
            codeStyleTrailingComma = preferences.codeStyle.trailingComma,
            theme = preferences.theme,
            responseVerbosity = preferences.responseVerbosity.name,
            customInstructions = if (preferences.customInstructions.isNotEmpty()) {
                preferences.customInstructions.joinToString("|||")
            } else null,
            totalMessages = interactionStats.totalMessages,
            totalSessions = interactionStats.totalSessions,
            totalTasksCompleted = interactionStats.totalTasksCompleted,
            averageSessionLength = interactionStats.averageSessionLength,
            mostUsedTaskTypes = serializeTaskTypesMap(interactionStats.mostUsedTaskTypes),
            lastActiveAt = interactionStats.lastActiveAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // === Context Anchor ===

    fun ContextAnchorEntity.toDomain(): ContextAnchor {
        return ContextAnchor(
            id = id,
            name = name,
            type = AnchorType.valueOf(type),
            path = path,
            topic = topic,
            context = context,
            priority = priority,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastUsedAt = lastUsedAt
        )
    }

    fun ContextAnchor.toEntity(): ContextAnchorEntity {
        return ContextAnchorEntity(
            id = id,
            name = name,
            type = type.name,
            path = path,
            topic = topic,
            context = context,
            priority = priority,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastUsedAt = lastUsedAt
        )
    }

    fun List<ContextAnchorEntity>.toAnchorDomain(): List<ContextAnchor> {
        return this.map { it.toDomain() }
    }

    // === Helper methods for serialization ===

    private fun parseTaskTypesMap(json: String?): Map<String, Int> {
        if (json.isNullOrBlank()) return emptyMap()
        return json.split(",")
            .filter { it.contains(":") }
            .associate {
                val parts = it.split(":")
                parts[0] to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
            }
    }

    private fun serializeTaskTypesMap(map: Map<String, Int>): String? {
        if (map.isEmpty()) return null
        return map.entries.joinToString(",") { "${it.key}:${it.value}" }
    }
}
