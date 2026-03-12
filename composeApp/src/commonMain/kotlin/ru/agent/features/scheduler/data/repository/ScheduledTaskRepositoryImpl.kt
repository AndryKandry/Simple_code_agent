package ru.agent.features.scheduler.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.agent.features.scheduler.data.local.dao.ScheduledTaskDao
import ru.agent.features.scheduler.data.local.entity.ScheduledTaskEntity
import ru.agent.features.scheduler.domain.model.ScheduledTask
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.model.TaskType
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository

/**
 * Implementation of ScheduledTaskRepository using Room DAO.
 */
class ScheduledTaskRepositoryImpl(
    private val dao: ScheduledTaskDao
) : ScheduledTaskRepository {

    override suspend fun getAll(): List<ScheduledTask> {
        return dao.getAll().map { it.toDomainModel() }
    }

    override fun getAllFlow(): Flow<List<ScheduledTask>> {
        return dao.getAllFlow().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getById(id: String): ScheduledTask? {
        return dao.getById(id)?.toDomainModel()
    }

    override fun getByIdFlow(id: String): Flow<ScheduledTask?> {
        return dao.getByIdFlow(id).map { entity ->
            entity?.toDomainModel()
        }
    }

    override suspend fun getByStatus(status: TaskStatus): List<ScheduledTask> {
        return dao.getByStatus(status.name).map { it.toDomainModel() }
    }

    override suspend fun getByType(type: TaskType): List<ScheduledTask> {
        return dao.getByType(type.name).map { it.toDomainModel() }
    }

    override suspend fun getDueTasks(now: Long): List<ScheduledTask> {
        return dao.getDueTasks(now).map { it.toDomainModel() }
    }

    override suspend fun create(task: ScheduledTask): ScheduledTask {
        val entity = ScheduledTaskEntity.fromDomainModel(task)
        dao.insert(entity)
        return task
    }

    override suspend fun update(task: ScheduledTask): ScheduledTask {
        val entity = ScheduledTaskEntity.fromDomainModel(task)
        dao.update(entity)
        return task
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun updateStatus(id: String, status: TaskStatus, updatedAt: Long) {
        dao.updateStatus(id, status.name, updatedAt)
    }

    override suspend fun updateRunTimestamps(id: String, nextRunAt: Long?, lastRunAt: Long, updatedAt: Long) {
        dao.updateRunTimestamps(id, nextRunAt, lastRunAt, updatedAt)
    }

    override suspend fun countByStatus(status: TaskStatus): Int {
        return dao.countByStatus(status.name)
    }

    override suspend fun count(): Int {
        return dao.count()
    }
}
