package ru.agent.features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.data.local.entity.MessageEntity

@Dao
interface MessageDao {

    /**
     * Получить все сообщения для сессии (одноразовый запрос).
     * Сортировка по времени (старые снизу, новые сверху).
     */
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: String): List<MessageEntity>

    /**
     * Получить все сообщения для сессии в виде Flow для реактивных обновлений.
     * Сортировка по времени (старые снизу, новые сверху).
     */
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSessionFlow(sessionId: String): Flow<List<MessageEntity>>

    /**
     * Вставить одно сообщение.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    /**
     * Вставить несколько сообщений (batch insert).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    /**
     * Удалить все сообщения для сессии.
     */
    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    /**
     * Обновить сообщение.
     */
    @Update
    suspend fun updateMessage(message: MessageEntity)

    /**
     * Получить количество сообщений в сессии.
     */
    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId")
    suspend fun getMessageCount(sessionId: String): Int

    /**
     * Получить последние N сообщений из сессии.
     * Полезно для загрузки контекста для AI или пагинации.
     */
    @Query(
        """
        SELECT * FROM messages
        WHERE sessionId = :sessionId
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    suspend fun getLastNMessages(sessionId: String, limit: Int): List<MessageEntity>

    /**
     * Получить сообщение по ID.
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    /**
     * Удалить сообщение по ID.
     */
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    /**
     * Получить сообщения за определенный период времени.
     */
    @Query(
        """
        SELECT * FROM messages
        WHERE sessionId = :sessionId
        AND timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp ASC
        """
    )
    suspend fun getMessagesInRange(
        sessionId: String,
        startTime: Long,
        endTime: Long
    ): List<MessageEntity>
}
