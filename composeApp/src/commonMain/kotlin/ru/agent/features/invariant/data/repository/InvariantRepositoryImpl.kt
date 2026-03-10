package ru.agent.features.invariant.data.repository

import co.touchlab.kermit.Logger
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.invariant.data.local.dao.InvariantDao
import ru.agent.features.invariant.data.local.mapper.InvariantMapper.toDomain
import ru.agent.features.invariant.data.local.mapper.InvariantMapper.toEntity
import ru.agent.features.invariant.domain.model.Invariant
import ru.agent.features.invariant.domain.model.InvariantFilter
import ru.agent.features.invariant.domain.model.InvariantListResult
import ru.agent.features.invariant.domain.model.InvariantSource
import ru.agent.features.invariant.domain.repository.InvariantRepository

/**
 * Реализация InvariantRepository через Room Database.
 *
 * @property invariantDao DAO для работы с БД
 * @property defaultInvariantsProvider Provider для дефолтных системных инвариантов
 */
class InvariantRepositoryImpl(
    private val invariantDao: InvariantDao,
    private val defaultInvariantsProvider: () -> List<Invariant>
) : InvariantRepository {

    private val logger = Logger.withTag("InvariantRepository")

    /**
     * Кэш инвариантов в памяти для быстрого доступа.
     */
    private var invariantsCache: MutableList<Invariant>? = null

    /**
     * Флаг инициализации системных инвариантов.
     */
    private var isInitialized = false

    /**
     * Инициализация системных инвариантов при первом запуске.
     */
    private suspend fun initializeDefaultInvariantsIfNeeded() {
        if (isInitialized) return

        val systemCount = invariantDao.countBySource(InvariantSource.SYSTEM.name)
        if (systemCount == 0) {
            logger.i { "Initializing default system invariants" }
            val defaultInvariants = defaultInvariantsProvider()
            invariantDao.insertAll(defaultInvariants.map { it.toEntity() })
            logger.i { "Initialized ${defaultInvariants.size} system invariants" }
        }
        isInitialized = true
    }

    override suspend fun getAllInvariants(): List<Invariant> {
        logger.d { "Getting all invariants" }

        // Инициализируем системные инварианты если нужно
        initializeDefaultInvariantsIfNeeded()

        val entities = invariantDao.getAll()
        val invariants = entities.toDomain()

        // Обновляем кэш
        invariantsCache = invariants.toMutableList()

        logger.d { "Found ${invariants.size} invariants" }
        return invariants
    }

    override suspend fun getInvariants(filter: InvariantFilter?): InvariantListResult {
        logger.d { "Getting invariants with filter: $filter" }

        val allInvariants = getAllInvariants()
        val totalCount = allInvariants.size

        val filtered = if (filter != null) {
            filter.apply(allInvariants)
        } else {
            allInvariants
        }

        return InvariantListResult(
            invariants = filtered,
            totalCount = totalCount,
            filteredCount = filtered.size,
            appliedFilter = filter
        )
    }

    override suspend fun getInvariantById(id: String): Invariant? {
        logger.d { "Getting invariant by id: $id" }
        return invariantDao.getById(id)?.toDomain()
    }

    override suspend fun addInvariant(invariant: Invariant): Result<Invariant> {
        return try {
            logger.i { "Adding invariant: ${invariant.id}" }

            // Проверяем на дублирование
            if (invariantDao.exists(invariant.id)) {
                return Result.failure(
                    IllegalArgumentException("Invariant with id '${invariant.id}' already exists")
                )
            }

            // Сохраняем в БД
            invariantDao.insert(invariant.toEntity())

            // Обновляем кэш
            invariantsCache?.add(invariant)

            logger.i { "Invariant added successfully: ${invariant.id}" }
            Result.success(invariant)
        } catch (e: Exception) {
            logger.e(e) { "Failed to add invariant" }
            Result.failure(e)
        }
    }

    override suspend fun removeInvariant(id: String): Result<Unit> {
        return try {
            logger.i { "Removing invariant: $id" }

            // Получаем инвариант для проверки
            val invariant = getInvariantById(id)
            if (invariant == null) {
                return Result.failure(
                    IllegalArgumentException("Invariant with id '$id' not found")
                )
            }

            // Проверяем возможность удаления
            if (!invariant.isDeletable()) {
                return Result.failure(
                    IllegalStateException(
                        "Cannot delete system CRITICAL invariant. Disable it instead."
                    )
                )
            }

            // Удаляем из БД
            invariantDao.deleteById(id)

            // Обновляем кэш
            invariantsCache?.removeAll { it.id == id }

            logger.i { "Invariant removed successfully: $id" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Failed to remove invariant" }
            Result.failure(e)
        }
    }

    override suspend fun toggleInvariant(id: String): Result<Invariant> {
        return try {
            logger.i { "Toggling invariant: $id" }

            val invariant = getInvariantById(id)
            if (invariant == null) {
                return Result.failure(
                    IllegalArgumentException("Invariant with id '$id' not found")
                )
            }

            // Создаем обновленный инвариант
            val updated = invariant.copy(
                isActive = !invariant.isActive,
                updatedAt = currentTimeMillis()
            )

            // Сохраняем в БД
            invariantDao.update(updated.toEntity())

            // Обновляем кэш
            invariantsCache?.let { cache ->
                val index = cache.indexOfFirst { it.id == id }
                if (index >= 0) {
                    cache[index] = updated
                }
            }

            logger.i { "Invariant toggled: $id -> active=${updated.isActive}" }
            Result.success(updated)
        } catch (e: Exception) {
            logger.e(e) { "Failed to toggle invariant" }
            Result.failure(e)
        }
    }

    override suspend fun updateInvariant(invariant: Invariant): Result<Invariant> {
        return try {
            logger.i { "Updating invariant: ${invariant.id}" }

            // Проверяем существование
            if (!invariantDao.exists(invariant.id)) {
                return Result.failure(
                    IllegalArgumentException("Invariant with id '${invariant.id}' not found")
                )
            }

            // Создаем обновленный инвариант
            val updated = invariant.copy(
                updatedAt = currentTimeMillis()
            )

            // Сохраняем в БД
            invariantDao.update(updated.toEntity())

            // Обновляем кэш
            invariantsCache?.let { cache ->
                val index = cache.indexOfFirst { it.id == invariant.id }
                if (index >= 0) {
                    cache[index] = updated
                }
            }

            logger.i { "Invariant updated successfully: ${invariant.id}" }
            Result.success(updated)
        } catch (e: Exception) {
            logger.e(e) { "Failed to update invariant" }
            Result.failure(e)
        }
    }

    override suspend fun getActiveInvariants(): List<Invariant> {
        logger.d { "Getting active invariants" }

        // Инициализируем системные инварианты если нужно
        initializeDefaultInvariantsIfNeeded()

        val entities = invariantDao.getActive()
        return entities.toDomain()
    }

    override suspend fun invariantExists(id: String): Boolean {
        return invariantDao.exists(id)
    }

    override suspend fun clearUserInvariants(): Int {
        logger.i { "Clearing user invariants" }

        // Получаем количество перед удалением
        val count = invariantDao.countBySource(InvariantSource.USER.name)

        // Удаляем пользовательские инварианты
        invariantDao.deleteUserInvariants()

        // Обновляем кэш
        invariantsCache?.removeAll { it.source == InvariantSource.USER }

        logger.i { "Cleared $count user invariants" }
        return count
    }
}
