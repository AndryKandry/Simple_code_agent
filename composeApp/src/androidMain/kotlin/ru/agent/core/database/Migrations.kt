package ru.agent.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Миграция с версии 6 на версию 7.
 *
 * Создает таблицу invariants для хранения правил проекта.
 */
val MIGRATION_6_7_ANDROID = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Создаем таблицу invariants
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS invariants (
                id TEXT NOT NULL PRIMARY KEY,
                description TEXT NOT NULL,
                category TEXT NOT NULL,
                priority TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                source TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Создаем индексы для быстрого поиска
        database.execSQL("CREATE INDEX IF NOT EXISTS index_invariants_category ON invariants(category)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_invariants_priority ON invariants(priority)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_invariants_source ON invariants(source)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_invariants_isActive ON invariants(isActive)")
    }
}

/**
 * Миграция с версии 7 на версию 8.
 *
 * Добавляет поля для сохранения результатов выполнения и валидации задачи.
 */
val MIGRATION_7_8_ANDROID = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Добавляем недостающие поля в таблицу task_states
        database.execSQL("ALTER TABLE task_states ADD COLUMN waitingForUserInput INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE task_states ADD COLUMN userFeedback TEXT")
        database.execSQL("ALTER TABLE task_states ADD COLUMN executionResult TEXT")
        database.execSQL("ALTER TABLE task_states ADD COLUMN validationResult TEXT")
        database.execSQL("ALTER TABLE task_states ADD COLUMN summary TEXT")
    }
}

/**
 * Миграция с версии 8 на версию 9.
 *
 * Создает таблицы для планировщика задач (scheduler).
 */
val MIGRATION_8_9_ANDROID = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Создаем таблицу scheduled_tasks
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS scheduled_tasks (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                cronExpression TEXT NOT NULL,
                taskType TEXT NOT NULL,
                taskDataJson TEXT NOT NULL,
                status TEXT NOT NULL,
                nextRunAt INTEGER,
                lastRunAt INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                tagsJson TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Создаем индексы для scheduled_tasks
        database.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_tasks_status ON scheduled_tasks(status)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_tasks_taskType ON scheduled_tasks(taskType)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_tasks_nextRunAt ON scheduled_tasks(nextRunAt)")

        // Создаем таблицу task_executions
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS task_executions (
                id TEXT NOT NULL PRIMARY KEY,
                taskId TEXT NOT NULL,
                startedAt INTEGER NOT NULL,
                completedAt INTEGER,
                status TEXT NOT NULL,
                result TEXT,
                error TEXT,
                FOREIGN KEY (taskId) REFERENCES scheduled_tasks(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Создаем индексы для task_executions
        database.execSQL("CREATE INDEX IF NOT EXISTS index_task_executions_taskId ON task_executions(taskId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_task_executions_status ON task_executions(status)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_task_executions_startedAt ON task_executions(startedAt)")
    }
}

/**
 * Миграция с версии 9 на версию 10.
 *
 * Создает таблицы для RAG системы (document_chunks, embeddings, index_metadata).
 */
val MIGRATION_9_10_ANDROID = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Создаем таблицу document_chunks
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS document_chunks (
                chunkId TEXT NOT NULL PRIMARY KEY,
                content TEXT NOT NULL,
                source TEXT NOT NULL,
                fileName TEXT NOT NULL,
                language TEXT NOT NULL,
                startLine INTEGER NOT NULL,
                endLine INTEGER NOT NULL,
                section TEXT,
                tokenCount INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Создаем индексы для document_chunks
        database.execSQL("CREATE INDEX IF NOT EXISTS index_document_chunks_source ON document_chunks(source)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_document_chunks_fileName ON document_chunks(fileName)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_document_chunks_language ON document_chunks(language)")

        // Создаем таблицу embeddings с Foreign Key
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS embeddings (
                id TEXT NOT NULL PRIMARY KEY,
                chunkId TEXT NOT NULL,
                embeddingJson TEXT NOT NULL,
                dimension INTEGER NOT NULL,
                model TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY (chunkId) REFERENCES document_chunks(chunkId) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Создаем индекс для embeddings
        database.execSQL("CREATE INDEX IF NOT EXISTS index_embeddings_chunkId ON embeddings(chunkId)")

        // Создаем таблицу index_metadata
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS index_metadata (
                id TEXT NOT NULL PRIMARY KEY,
                strategy TEXT NOT NULL,
                totalChunks INTEGER NOT NULL,
                totalFiles INTEGER NOT NULL,
                avgTokensPerChunk REAL NOT NULL,
                minTokens INTEGER NOT NULL,
                maxTokens INTEGER NOT NULL,
                stdDevTokens REAL NOT NULL,
                durationMs INTEGER NOT NULL,
                indexedAt INTEGER NOT NULL,
                model TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

/**
 * Список всех миграций для Android.
 */
val ALL_MIGRATIONS_ANDROID = listOf(
    MIGRATION_6_7_ANDROID,
    MIGRATION_7_8_ANDROID,
    MIGRATION_8_9_ANDROID,
    MIGRATION_9_10_ANDROID
)
