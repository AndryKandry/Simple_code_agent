package ru.agent.features.scheduler.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.agent.features.scheduler.data.local.dao.TaskExecutionDao
import ru.agent.features.scheduler.data.local.entity.TaskExecutionEntity
import ru.agent.features.scheduler.domain.model.ExecutionStatus
import ru.agent.features.scheduler.domain.model.TaskExecution
import ru.agent.features.scheduler.domain.repository.TaskExecutionRepository

/**
 * Implementation of TaskExecutionRepository using Room DAO.
 */
class TaskExecutionRepositoryImpl(
    private val dao: TaskExecutionDao
) : TaskExecutionRepository {

    override suspend fun getAll(): List<TaskExecution> {
        return dao.getAll().map { it.toDomainModel() }
    }

    override suspend fun getById(id: String): TaskExecution? {
        return dao.getById(id)?.toDomainModel()
    }

    override suspend fun getByTaskId(taskId: String): List<TaskExecution> {
        return dao.getByTaskId(taskId).map { it.toDomainModel() }
    }

    override fun getByTaskIdFlow(taskId: String): Flow<List<TaskExecution>> {
        return dao.getByTaskIdFlow(taskId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getRecent(limit: Int): List<TaskExecution> {
        return dao.getRecent(limit).map { it.toDomainModel() }
    }

    override suspend fun getByStatus(status: ExecutionStatus): List<TaskExecution> {
        return dao.getByStatus(status.name).map { it.toDomainModel() }
    }

    override suspend fun getByTimeRange(start: Long, end: Long): List<TaskExecution> {
        return dao.getByTimeRange(start, end).map { it.toDomainModel() }
    }

    override suspend fun create(execution: TaskExecution): TaskExecution {
        val entity = TaskExecutionEntity.fromDomainModel(execution)
        dao.insert(entity)
        return execution
    }

    override suspend fun update(execution: TaskExecution): TaskExecution {
        val entity = TaskExecutionEntity.fromDomainModel(execution)
        dao.update(entity)
        return execution
    }

    override suspend fun complete(
        id: String,
        completedAt: Long,
        status: ExecutionStatus,
        result: String?,
        error: String?
    ) {
        dao.complete(id, completedAt, status.name, result, error)
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun deleteByTaskId(taskId: String) {
        dao.deleteByTaskId(taskId)
    }

    override suspend fun countByTaskId(taskId: String): Int {
        return dao.countByTaskId(taskId)
    }

    override suspend fun count(): Int {
        return dao.count()
    }
}
