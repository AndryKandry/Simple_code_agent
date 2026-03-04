package ru.agent.features.task.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.agent.features.task.data.local.dao.TaskStateDao
import ru.agent.features.task.data.local.entity.TaskStateEntity
import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.repository.TaskStateRepository

/**
 * Implementation of TaskStateRepository using Room database.
 */
class TaskStateRepositoryImpl(
    private val taskStateDao: TaskStateDao
) : TaskStateRepository {

    override suspend fun getTaskState(taskId: String): TaskState? {
        return taskStateDao.getTaskStateById(taskId)?.toDomainModel()
    }

    override suspend fun getActiveTaskForSession(sessionId: String): TaskState? {
        return taskStateDao.getActiveTaskForSession(sessionId)?.toDomainModel()
    }

    override fun getTaskStateFlow(taskId: String): Flow<TaskState?> {
        return taskStateDao.getTaskStateByIdFlow(taskId)
            .map { entity -> entity?.toDomainModel() }
    }

    override fun getActiveTaskFlowForSession(sessionId: String): Flow<TaskState?> {
        return taskStateDao.getActiveTaskForSessionFlow(sessionId)
            .map { entity -> entity?.toDomainModel() }
    }

    override suspend fun saveTaskState(taskState: TaskState) {
        val entity = TaskStateEntity.fromDomainModel(taskState)
        taskStateDao.insertTaskState(entity)
    }

    override suspend fun deleteTaskState(taskId: String) {
        taskStateDao.deleteTaskById(taskId)
    }

    override suspend fun deleteCompletedTasksForSession(sessionId: String) {
        taskStateDao.deleteCompletedTasksForSession(sessionId)
    }

    override suspend fun getAllTasksForSession(sessionId: String): List<TaskState> {
        return taskStateDao.getAllTasksForSession(sessionId)
            .map { it.toDomainModel() }
    }
}
