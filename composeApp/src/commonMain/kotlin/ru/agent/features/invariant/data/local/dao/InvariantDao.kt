package ru.agent.features.invariant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.invariant.data.local.entity.InvariantEntity

/**
 * DAO для работы с инвариантами.
 */
@Dao
interface InvariantDao {

    /**
     * Получить все инварианты.
     */
    @Query("SELECT * FROM invariants ORDER BY priority DESC, createdAt ASC")
    suspend fun getAll(): List<InvariantEntity>

    /**
     * Получить все инварианты как Flow.
     */
    @Query("SELECT * FROM invariants ORDER BY priority DESC, createdAt ASC")
    fun getAllFlow(): Flow<List<InvariantEntity>>

    /**
     * Получить инвариант по ID.
     */
    @Query("SELECT * FROM invariants WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InvariantEntity?

    /**
     * Получить инвариант по ID как Flow.
     */
    @Query("SELECT * FROM invariants WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: String): Flow<InvariantEntity?>

    /**
     * Получить только активные инварианты.
     */
    @Query("SELECT * FROM invariants WHERE isActive = 1 ORDER BY priority DESC, createdAt ASC")
    suspend fun getActive(): List<InvariantEntity>

    /**
     * Получить только активные инварианты как Flow.
     */
    @Query("SELECT * FROM invariants WHERE isActive = 1 ORDER BY priority DESC, createdAt ASC")
    fun getActiveFlow(): Flow<List<InvariantEntity>>

    /**
     * Получить инварианты по категории.
     */
    @Query("SELECT * FROM invariants WHERE category = :category ORDER BY priority DESC, createdAt ASC")
    suspend fun getByCategory(category: String): List<InvariantEntity>

    /**
     * Получить инварианты по приоритету.
     */
    @Query("SELECT * FROM invariants WHERE priority = :priority ORDER BY createdAt ASC")
    suspend fun getByPriority(priority: String): List<InvariantEntity>

    /**
     * Получить инварианты по источнику.
     */
    @Query("SELECT * FROM invariants WHERE source = :source ORDER BY priority DESC, createdAt ASC")
    suspend fun getBySource(source: String): List<InvariantEntity>

    /**
     * Вставить или обновить инвариант.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invariant: InvariantEntity)

    /**
     * Вставить несколько инвариантов.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invariants: List<InvariantEntity>)

    /**
     * Обновить инвариант.
     */
    @Update
    suspend fun update(invariant: InvariantEntity)

    /**
     * Удалить инвариант по ID.
     */
    @Query("DELETE FROM invariants WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Удалить все пользовательские инварианты.
     */
    @Query("DELETE FROM invariants WHERE source = 'USER'")
    suspend fun deleteUserInvariants()

    /**
     * Проверить существование инварианта.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM invariants WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    /**
     * Получить количество инвариантов.
     */
    @Query("SELECT COUNT(*) FROM invariants")
    suspend fun count(): Int

    /**
     * Получить количество инвариантов по источнику.
     */
    @Query("SELECT COUNT(*) FROM invariants WHERE source = :source")
    suspend fun countBySource(source: String): Int
}
