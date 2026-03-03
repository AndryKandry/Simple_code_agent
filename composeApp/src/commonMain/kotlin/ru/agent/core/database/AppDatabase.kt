package ru.agent.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import ru.agent.features.chat.data.local.dao.ChatSessionDao
import ru.agent.features.chat.data.local.dao.MessageDao
import ru.agent.features.chat.data.local.entity.ChatSessionEntity
import ru.agent.features.chat.data.local.entity.MessageEntity
import ru.agent.features.memory.data.local.dao.ContextAnchorDao
import ru.agent.features.memory.data.local.dao.KnowledgeEntryDao
import ru.agent.features.memory.data.local.dao.UserProfileDao
import ru.agent.features.memory.data.local.dao.WorkingMemoryDao
import ru.agent.features.memory.data.local.entity.ContextAnchorEntity
import ru.agent.features.memory.data.local.entity.KnowledgeEntryEntity
import ru.agent.features.memory.data.local.entity.UserProfileEntity
import ru.agent.features.memory.data.local.entity.WorkingMemoryEntity

@Database(
    entities = [
        TestEntity::class,
        ChatSessionEntity::class,
        MessageEntity::class,
        WorkingMemoryEntity::class,
        KnowledgeEntryEntity::class,
        UserProfileEntity::class,
        ContextAnchorEntity::class
    ],
    version = 3
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getTestDao(): TestDao
    abstract fun getChatSessionDao(): ChatSessionDao
    abstract fun getMessageDao(): MessageDao
    abstract fun getWorkingMemoryDao(): WorkingMemoryDao
    abstract fun getKnowledgeEntryDao(): KnowledgeEntryDao
    abstract fun getUserProfileDao(): UserProfileDao
    abstract fun getContextAnchorDao(): ContextAnchorDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

internal const val dbFileName = "defkmp.db"
