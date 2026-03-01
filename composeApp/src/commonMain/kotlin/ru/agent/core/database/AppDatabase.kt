package ru.agent.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import ru.agent.features.chat.data.local.dao.ChatSessionDao
import ru.agent.features.chat.data.local.dao.MessageDao
import ru.agent.features.chat.data.local.dao.MessageSummaryDao
import ru.agent.features.chat.data.local.dao.TokenUsageDao
import ru.agent.features.chat.data.local.entity.ChatSessionEntity
import ru.agent.features.chat.data.local.entity.MessageEntity
import ru.agent.features.chat.data.local.entity.MessageSummaryEntity
import ru.agent.features.chat.data.local.entity.TokenUsageEntity

@Database(
    entities = [
        TestEntity::class,
        ChatSessionEntity::class,
        MessageEntity::class,
        MessageSummaryEntity::class,
        TokenUsageEntity::class
    ],
    version = 3
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getTestDao(): TestDao
    abstract fun getChatSessionDao(): ChatSessionDao
    abstract fun getMessageDao(): MessageDao
    abstract fun getMessageSummaryDao(): MessageSummaryDao
    abstract fun getTokenUsageDao(): TokenUsageDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

internal const val dbFileName = "defkmp.db"
