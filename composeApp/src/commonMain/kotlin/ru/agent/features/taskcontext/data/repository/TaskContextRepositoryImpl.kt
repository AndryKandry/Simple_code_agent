package ru.agent.features.taskcontext.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.agent.features.taskcontext.data.local.dao.TaskContextDao
import ru.agent.features.taskcontext.data.local.entity.TaskContextEntity
import ru.agent.features.taskcontext.data.local.mapper.TaskContextMapper
import ru.agent.features.taskcontext.domain.model.TaskContext
import ru.agent.features.taskcontext.domain.repository.TaskContextRepository

/**
 * Реализация Repository для работы с контекстом задач.
 *
 * Использует Room Database для хранения и TaskContextMapper
 * для преобразования между Domain и Entity моделями.
 */
class TaskContextRepositoryImpl(
    private val taskContextDao: TaskContextDao,
    private val mapper: TaskContextMapper
) : TaskContextRepository {

    override suspend fun getBySessionId(sessionId: String): TaskContext? {
        val entity = taskContextDao.getBySessionId(sessionId)
        return entity?.let { mapper.toDomain(it) }
    }

    override suspend fun getById(id: String): TaskContext? {
        val entity = taskContextDao.getById(id)
        return entity?.let { mapper.toDomain(it) }
    }

    override fun observeBySessionId(sessionId: String): Flow<TaskContext?> {
        return taskContextDao.observeBySessionId(sessionId)
            .map { entity -> entity?.let { mapper.toDomain(it) } }
    }

    override suspend fun save(taskContext: TaskContext) {
        val existing = taskContextDao.getBySessionId(taskContext.sessionId)
        if (existing != null) {
            update(taskContext)
        } else {
            insert(taskContext)
        }
    }

    override suspend fun insert(taskContext: TaskContext) {
        val entity = mapper.toEntity(taskContext)
        taskContextDao.insert(entity)
    }

    override suspend fun update(taskContext: TaskContext) {
        val entity = mapper.toEntity(taskContext)
        taskContextDao.update(entity)
    }

    override suspend fun deleteBySessionId(sessionId: String): Int {
        return taskContextDao.deleteBySessionId(sessionId)
    }

    override suspend fun deleteById(id: String): Int {
        return taskContextDao.deleteById(id)
    }

    override suspend fun getAll(): List<TaskContext> {
        val entities = taskContextDao.getAll()
        return mapper.toDomainList(entities)
    }

    override suspend fun count(): Int {
        return taskContextDao.count()
    }
}
