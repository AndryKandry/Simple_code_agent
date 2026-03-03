package ru.agent.features.memory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.memory.data.local.entity.UserProfileEntity

/**
 * DAO для профиля пользователя.
 */
@Dao
interface UserProfileDao {

    /**
     * Получить профиль по ID.
     */
    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserProfileEntity?

    /**
     * Получить профиль по ID как Flow.
     */
    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: String): Flow<UserProfileEntity?>

    /**
     * Вставить или обновить профиль.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity)

    /**
     * Обновить профиль.
     */
    @Update
    suspend fun update(profile: UserProfileEntity)

    /**
     * Удалить профиль по ID.
     */
    @Query("DELETE FROM user_profiles WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Получить все профили.
     */
    @Query("SELECT * FROM user_profiles ORDER BY updatedAt DESC")
    suspend fun getAll(): List<UserProfileEntity>

    /**
     * Получить все профили как Flow.
     */
    @Query("SELECT * FROM user_profiles ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<UserProfileEntity>>

    /**
     * Обновить статистику взаимодействий.
     */
    @Query("""
        UPDATE user_profiles SET
            totalMessages = :totalMessages,
            totalSessions = :totalSessions,
            totalTasksCompleted = :totalTasksCompleted,
            averageSessionLength = :averageSessionLength,
            lastActiveAt = :lastActiveAt,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateStats(
        id: String,
        totalMessages: Long,
        totalSessions: Long,
        totalTasksCompleted: Long,
        averageSessionLength: Float,
        lastActiveAt: Long,
        updatedAt: Long
    )

    /**
     * Получить профиль по умолчанию.
     */
    @Query("SELECT * FROM user_profiles WHERE id = 'default' LIMIT 1")
    suspend fun getDefaultProfile(): UserProfileEntity?

    /**
     * Проверить существование профиля.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM user_profiles WHERE id = :id)")
    suspend fun exists(id: String): Boolean
}
