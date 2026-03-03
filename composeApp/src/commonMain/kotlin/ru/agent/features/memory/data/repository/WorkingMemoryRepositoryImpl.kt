package ru.agent.features.memory.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.memory.data.local.dao.WorkingMemoryDao
import ru.agent.features.memory.data.local.mapper.MemoryMapper.toDomain
import ru.agent.features.memory.data.local.mapper.MemoryMapper.toEntity
import ru.agent.features.memory.domain.model.ExecutionState
import ru.agent.features.memory.domain.model.TaskInfo
import ru.agent.features.memory.domain.model.TaskStatus
import ru.agent.features.memory.domain.model.WorkingMemory
import ru.agent.features.memory.domain.repository.WorkingMemoryRepository

/**
 * Repository implementation for Working Memory (WM)
 *
 * Stores current task data and execution state in Room Database.
 */
class WorkingMemoryRepositoryImpl(
    private val workingMemoryDao: WorkingMemoryDao,
    private val logger: Logger = Logger.withTag("WorkingMemoryRepository")
) : WorkingMemoryRepository {

    override suspend fun getWorkingMemory(sessionId: String): WorkingMemory? {
        return try {
            withContext(Dispatchers.IO) {
                val entity = workingMemoryDao.getBySessionId(sessionId)
                entity?.toDomain()
            }
        } catch (e: Exception) {
            logger.e(throwable = e) { "Error getting working memory" }
            null
        }
    }

    override fun getWorkingMemoryFlow(sessionId: String): Flow<WorkingMemory?> {
        return workingMemoryDao.getBySessionIdFlow(sessionId)
            .map { it?.toDomain() }
    }

    override suspend fun saveWorkingMemory(workingMemory: WorkingMemory): ResultWrapper<WorkingMemory> {
        return try {
            withContext(Dispatchers.IO) {
                val entity = workingMemory.toEntity()
                workingMemoryDao.insert(entity)
                logger.i { "Working memory saved for session: ${workingMemory.sessionId}" }
                ResultWrapper.Success(workingMemory)
            }
        } catch (e: Exception) {
            logger.e(throwable = e) { "Error saving working memory" }
            ResultWrapper.Error(
                throwable = e,
                message = "Failed to save working memory"
            )
        }
    }

    override suspend fun updateExecutionState(sessionId: String, state: ExecutionState) {
        try {
            withContext(Dispatchers.IO) {
                val existing = workingMemoryDao.getBySessionId(sessionId)
                if (existing == null) {
                    logger.w { "No working memory found for session: $sessionId" }
                    return@withContext
                }
                val now = currentTimeMillis()
                workingMemoryDao.updateExecutionState(sessionId, state.name, now)
                logger.d { "Execution state updated: $state" }
            }
        } catch (e: Exception) {
            logger.e(throwable = e) { "Error updating execution state" }
        }
    }

    override suspend fun setTask(sessionId: String, taskInfo: TaskInfo) {
        try {
            withContext(Dispatchers.IO) {
                val existing = workingMemoryDao.getBySessionId(sessionId)
                if (existing == null) {
                    logger.w { "No working memory found for session: $sessionId" }
                    return@withContext
                }
                val entity = existing.copy(
                    taskId = taskInfo.taskId,
                    taskType = taskInfo.taskType.name,
                    taskDescription = taskInfo.description,
                    taskStatus = taskInfo.status.name,
                    taskProgress = taskInfo.progress,
                    taskStartedAt = taskInfo.startedAt,
                    taskCompletedAt = taskInfo.completedAt,
                    parentTaskId = taskInfo.parentTaskId
                )
                workingMemoryDao.update(entity)
                logger.d { "Task set: ${taskInfo.taskId}" }
            }
        } catch (e: Exception) {
            logger.e(throwable = e) { "Error setting task" }
        }
    }

    override suspend fun updateTaskStatus(sessionId: String, status: TaskStatus, progress: Float) {
        try {
            withContext(Dispatchers.IO) {
                val existing = workingMemoryDao.getBySessionId(sessionId)
                if (existing == null) {
                    logger.w { "No working memory found for session: $sessionId" }
                    return@withContext
                }
                val entity = existing.copy(
                    taskStatus = status.name,
                    taskProgress = progress,
                    updatedAt = currentTimeMillis()
                )
                workingMemoryDao.update(entity)
                logger.d { "Task status updated: $status" }
            }
        } catch (e: Exception) {
            logger.e(throwable = e) { "Error updating task status" }
        }
    }

    override suspend fun saveTemporaryData(sessionId: String, data: String) {
        try {
            withContext(Dispatchers.IO) {
                val existing = workingMemoryDao.getBySessionId(sessionId)
                if (existing == null) {
                    logger.w { "No working memory found for session: $sessionId" }
                    return@withContext
                }
                val now = currentTimeMillis()
                val entity = existing.copy(
                    temporaryData = data,
                    updatedAt = now
                )
                workingMemoryDao.update(entity)
                logger.d { "Temporary data saved" }
            }
        } catch (e: Exception) {
            logger.e(throwable = e) { "Error saving temporary data" }
        }
    }

    override suspend fun getTemporaryData(sessionId: String): String? {
        return try {
            withContext(Dispatchers.IO) {
                val existing = workingMemoryDao.getBySessionId(sessionId)
                existing?.temporaryData
            }
        } catch (e: Exception) {
            logger.e(throwable = e) { "Error getting temporary data" }
            null
        }
    }

    override suspend fun clearWorkingMemory(sessionId: String) {
        try {
            withContext(Dispatchers.IO) {
                workingMemoryDao.deleteBySessionId(sessionId)
                logger.i { "Working memory cleared for session: $sessionId" }
            }
        } catch (e: Exception) {
            logger.e(throwable = e) { "Error clearing working memory" }
        }
    }

    override suspend fun getActiveTasks(): List<WorkingMemory> {
        return try {
            withContext(Dispatchers.IO) {
                val results = workingMemoryDao.getActiveTasks().map { it.toDomain() }
                logger.d { "Found ${results.size} active tasks" }
                results
            }
        } catch (e: Exception) {
            logger.e(throwable = e) { "Error getting active tasks" }
            emptyList()
        }
    }
}
