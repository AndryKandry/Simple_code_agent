package ru.agent.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import ru.agent.features.chat.data.local.dao.ChatSessionDao
import ru.agent.features.chat.data.local.dao.MessageDao
import ru.agent.features.chat.data.local.entity.ChatSessionEntity
import ru.agent.features.chat.data.local.entity.MessageEntity
import ru.agent.features.invariant.data.local.dao.InvariantDao
import ru.agent.features.invariant.data.local.entity.InvariantEntity
import ru.agent.features.memory.data.local.dao.ContextAnchorDao
import ru.agent.features.memory.data.local.dao.KnowledgeEntryDao
import ru.agent.features.memory.data.local.dao.UserProfileDao
import ru.agent.features.memory.data.local.dao.WorkingMemoryDao
import ru.agent.features.memory.data.local.entity.ContextAnchorEntity
import ru.agent.features.memory.data.local.entity.KnowledgeEntryEntity
import ru.agent.features.memory.data.local.entity.UserProfileEntity
import ru.agent.features.memory.data.local.entity.WorkingMemoryEntity
import ru.agent.features.task.data.local.dao.TaskStateDao
import ru.agent.features.task.data.local.entity.TaskStateEntity
import ru.agent.features.scheduler.data.local.dao.ScheduledTaskDao
import ru.agent.features.scheduler.data.local.dao.TaskExecutionDao
import ru.agent.features.scheduler.data.local.entity.ScheduledTaskEntity
import ru.agent.features.scheduler.data.local.entity.TaskExecutionEntity
import ru.agent.features.rag.data.local.dao.DocumentChunkDao
import ru.agent.features.rag.data.local.dao.EmbeddingDao
import ru.agent.features.rag.data.local.dao.IndexMetadataDao
import ru.agent.features.rag.data.local.entity.DocumentChunkEntity
import ru.agent.features.rag.data.local.entity.EmbeddingEntity
import ru.agent.features.rag.data.local.entity.IndexMetadataEntity
import ru.agent.features.taskcontext.data.local.dao.TaskContextDao
import ru.agent.features.taskcontext.data.local.entity.TaskContextEntity

@Database(
    entities = [
        TestEntity::class,
        ChatSessionEntity::class,
        MessageEntity::class,
        WorkingMemoryEntity::class,
        KnowledgeEntryEntity::class,
        UserProfileEntity::class,
        ContextAnchorEntity::class,
        TaskStateEntity::class,
        InvariantEntity::class,
        ScheduledTaskEntity::class,
        TaskExecutionEntity::class,
        DocumentChunkEntity::class,
        EmbeddingEntity::class,
        IndexMetadataEntity::class,
        TaskContextEntity::class
    ],
    version = 12
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
    abstract fun getTaskStateDao(): TaskStateDao
    abstract fun getInvariantDao(): InvariantDao
    abstract fun getScheduledTaskDao(): ScheduledTaskDao
    abstract fun getTaskExecutionDao(): TaskExecutionDao
    abstract fun getDocumentChunkDao(): DocumentChunkDao
    abstract fun getEmbeddingDao(): EmbeddingDao
    abstract fun getIndexMetadataDao(): IndexMetadataDao
    abstract fun getTaskContextDao(): TaskContextDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

internal const val dbFileName = "defkmp.db"
