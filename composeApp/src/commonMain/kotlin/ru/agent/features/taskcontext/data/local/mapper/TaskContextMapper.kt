package ru.agent.features.taskcontext.data.local.mapper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.agent.features.taskcontext.data.local.entity.TaskContextEntity
import ru.agent.features.taskcontext.domain.model.Clarification
import ru.agent.features.taskcontext.domain.model.Constraint
import ru.agent.features.taskcontext.domain.model.RagQueryHistory
import ru.agent.features.taskcontext.domain.model.TaskContext

/**
 * Mapper для преобразования между Domain моделью и Entity.
 *
 * Использует JSON сериализацию для хранения сложных объектов (списки)
 * в текстовых полях базы данных.
 */
class TaskContextMapper(
    private val json: Json
) {

    /**
     * Преобразовать Entity в Domain модель.
     */
    fun toDomain(entity: TaskContextEntity): TaskContext {
        return TaskContext(
            id = entity.id,
            sessionId = entity.sessionId,
            goal = entity.goal,
            clarifications = decodeClarifications(entity.clarifications),
            constraints = decodeConstraints(entity.constraints),
            ragQueries = decodeRagQueries(entity.ragQueries),
            stage = decodeStage(entity.stage),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Преобразовать Domain модель в Entity.
     */
    fun toEntity(domain: TaskContext): TaskContextEntity {
        return TaskContextEntity(
            id = domain.id,
            sessionId = domain.sessionId,
            goal = domain.goal,
            clarifications = encodeClarifications(domain.clarifications),
            constraints = encodeConstraints(domain.constraints),
            ragQueries = encodeRagQueries(domain.ragQueries),
            stage = encodeStage(domain.stage),
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    /**
     * Преобразовать список Entity в список Domain моделей.
     */
    fun toDomainList(entities: List<TaskContextEntity>): List<TaskContext> {
        return entities.map { toDomain(it) }
    }

    // === Private helper methods ===

    private fun decodeClarifications(jsonString: String): List<Clarification> {
        return if (jsonString.isBlank()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<Clarification>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun encodeClarifications(clarifications: List<Clarification>): String {
        return if (clarifications.isEmpty()) {
            ""
        } else {
            try {
                json.encodeToString(clarifications)
            } catch (e: Exception) {
                ""
            }
        }
    }

    private fun decodeConstraints(jsonString: String): List<Constraint> {
        return if (jsonString.isBlank()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<Constraint>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun encodeConstraints(constraints: List<Constraint>): String {
        return if (constraints.isEmpty()) {
            ""
        } else {
            try {
                json.encodeToString(constraints)
            } catch (e: Exception) {
                ""
            }
        }
    }

    private fun decodeRagQueries(jsonString: String): List<RagQueryHistory> {
        return if (jsonString.isBlank()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<RagQueryHistory>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun encodeRagQueries(queries: List<RagQueryHistory>): String {
        return if (queries.isEmpty()) {
            ""
        } else {
            try {
                json.encodeToString(queries)
            } catch (e: Exception) {
                ""
            }
        }
    }

    private fun decodeStage(stageString: String): ru.agent.features.taskcontext.domain.model.ContextStage {
        return try {
            ru.agent.features.taskcontext.domain.model.ContextStage.valueOf(stageString)
        } catch (e: Exception) {
            ru.agent.features.taskcontext.domain.model.ContextStage.INITIALIZING
        }
    }

    private fun encodeStage(stage: ru.agent.features.taskcontext.domain.model.ContextStage): String {
        return stage.name
    }
}
