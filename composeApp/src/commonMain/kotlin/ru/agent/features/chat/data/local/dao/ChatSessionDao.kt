package ru.agent.features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.data.local.entity.ChatSessionEntity

@Dao
interface ChatSessionDao {

    /**
     * Получить все сессии в виде Flow для реактивных обновлений.
     * Сортировка по дате последнего обновления (новые сверху).
     */
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessionsFlow(): Flow<List<ChatSessionEntity>>

    /**
     * Получить сессию по ID (одноразовый запрос).
     */
    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): ChatSessionEntity?

    /**
     * Получить сессию по ID в виде Flow для реактивных обновлений.
     */
    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    fun getSessionByIdFlow(sessionId: String): Flow<ChatSessionEntity?>

    /**
     * Вставить новую сессию.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    /**
     * Обновить существующую сессию.
     */
    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    /**
     * Удалить сессию по ID.
     */
    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    /**
     * Инкрементировать счетчик сообщений и обновить timestamp сессии.
     * Используется при добавлении нового сообщения в чат.
     */
    @Query(
        """
        UPDATE chat_sessions
        SET messageCount = messageCount + 1,
            updatedAt = :timestamp
        WHERE id = :sessionId
        """
    )
    suspend fun incrementMessageCount(sessionId: String, timestamp: Long)

    /**
     * Получить количество сессий.
     */
    @Query("SELECT COUNT(*) FROM chat_sessions")
    suspend fun getSessionCount(): Int

    /**
     * Получить все неархивированные сессии.
     */
    @Query("SELECT * FROM chat_sessions WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveSessionsFlow(): Flow<List<ChatSessionEntity>>

    /**
     * Архивировать сессию.
     */
    @Query("UPDATE chat_sessions SET isArchived = 1 WHERE id = :sessionId")
    suspend fun archiveSession(sessionId: String)

    /**
     * Разархивировать сессию.
     */
    @Query("UPDATE chat_sessions SET isArchived = 0 WHERE id = :sessionId")
    suspend fun unarchiveSession(sessionId: String)
}
