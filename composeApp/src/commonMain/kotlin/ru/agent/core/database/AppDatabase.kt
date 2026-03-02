package ru.agent.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import ru.agent.features.chat.data.local.dao.BranchDao
import ru.agent.features.chat.data.local.dao.ChatSessionDao
import ru.agent.features.chat.data.local.dao.CheckpointDao
import ru.agent.features.chat.data.local.dao.FactDao
import ru.agent.features.chat.data.local.dao.MessageDao
import ru.agent.features.chat.data.local.entity.BranchEntity
import ru.agent.features.chat.data.local.entity.ChatSessionEntity
import ru.agent.features.chat.data.local.entity.CheckpointEntity
import ru.agent.features.chat.data.local.entity.FactEntity
import ru.agent.features.chat.data.local.entity.MessageEntity

@Database(
    entities = [
        TestEntity::class,
        ChatSessionEntity::class,
        MessageEntity::class,
        FactEntity::class,
        CheckpointEntity::class,
        BranchEntity::class
    ],
    version = 4
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getTestDao(): TestDao
    abstract fun getChatSessionDao(): ChatSessionDao
    abstract fun getMessageDao(): MessageDao
    abstract fun getFactDao(): FactDao
    abstract fun getCheckpointDao(): CheckpointDao
    abstract fun getBranchDao(): BranchDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

internal const val dbFileName = "defkmp.db"
