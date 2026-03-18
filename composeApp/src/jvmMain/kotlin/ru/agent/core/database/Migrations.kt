package ru.agent.core.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection

/**
 * Миграция с версии 6 на версию 7.
 *
 * Создает таблицу invariants для хранения правил проекта.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        // Создаем таблицу invariants
        connection.prepare(
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
        ).use { statement ->
            statement.step()
        }

        // Создаем индексы для быстрого поиска
        listOf(
            "CREATE INDEX IF NOT EXISTS index_invariants_category ON invariants(category)",
            "CREATE INDEX IF NOT EXISTS index_invariants_priority ON invariants(priority)",
            "CREATE INDEX IF NOT EXISTS index_invariants_source ON invariants(source)",
            "CREATE INDEX IF NOT EXISTS index_invariants_isActive ON invariants(isActive)"
        ).forEach { sql ->
            connection.prepare(sql).use { statement ->
                statement.step()
            }
        }
    }
}

/**
 * Миграция с версии 7 на версию 8.
 *
 * Добавляет поля для сохранения результатов выполнения и валидации задачи.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        // Добавляем недостающие поля в таблицу task_states
        listOf(
            "ALTER TABLE task_states ADD COLUMN waitingForUserInput INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE task_states ADD COLUMN userFeedback TEXT",
            "ALTER TABLE task_states ADD COLUMN executionResult TEXT",
            "ALTER TABLE task_states ADD COLUMN validationResult TEXT",
            "ALTER TABLE task_states ADD COLUMN summary TEXT"
        ).forEach { sql ->
            connection.prepare(sql).use { statement ->
                statement.step()
            }
        }
    }
}

/**
 * Миграция с версии 8 на версию 9.
 *
 * Создает таблицы для планировщика задач (scheduler).
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        // Создаем таблицу scheduled_tasks
        connection.prepare(
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
        ).use { statement ->
            statement.step()
        }

        // Создаем индексы для scheduled_tasks
        listOf(
            "CREATE INDEX IF NOT EXISTS index_scheduled_tasks_status ON scheduled_tasks(status)",
            "CREATE INDEX IF NOT EXISTS index_scheduled_tasks_taskType ON scheduled_tasks(taskType)",
            "CREATE INDEX IF NOT EXISTS index_scheduled_tasks_nextRunAt ON scheduled_tasks(nextRunAt)"
        ).forEach { sql ->
            connection.prepare(sql).use { statement ->
                statement.step()
            }
        }

        // Создаем таблицу task_executions
        connection.prepare(
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
        ).use { statement ->
            statement.step()
        }

        // Создаем индексы для task_executions
        listOf(
            "CREATE INDEX IF NOT EXISTS index_task_executions_taskId ON task_executions(taskId)",
            "CREATE INDEX IF NOT EXISTS index_task_executions_status ON task_executions(status)",
            "CREATE INDEX IF NOT EXISTS index_task_executions_startedAt ON task_executions(startedAt)"
        ).forEach { sql ->
            connection.prepare(sql).use { statement ->
                statement.step()
            }
        }
    }
}

/**
 * Миграция с версии 9 на версию 10.
 *
 * Создает таблицы для RAG системы (document_chunks, embeddings, index_metadata).
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        // Создаем таблицу document_chunks
        connection.prepare(
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
        ).use { statement ->
            statement.step()
        }

        // Создаем индексы для document_chunks
        listOf(
            "CREATE INDEX IF NOT EXISTS index_document_chunks_source ON document_chunks(source)",
            "CREATE INDEX IF NOT EXISTS index_document_chunks_fileName ON document_chunks(fileName)",
            "CREATE INDEX IF NOT EXISTS index_document_chunks_language ON document_chunks(language)"
        ).forEach { sql ->
            connection.prepare(sql).use { statement ->
                statement.step()
            }
        }

        // Создаем таблицу embeddings с Foreign Key
        connection.prepare(
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
        ).use { statement ->
            statement.step()
        }

        // Создаем индекс для embeddings
        listOf(
            "CREATE INDEX IF NOT EXISTS index_embeddings_chunkId ON embeddings(chunkId)"
        ).forEach { sql ->
            connection.prepare(sql).use { statement ->
                statement.step()
            }
        }

        // Создаем таблицу index_metadata
        connection.prepare(
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
        ).use { statement ->
            statement.step()
        }
    }
}

/**
 * Список всех миграций для добавления в Room database builder.
 */
val ALL_MIGRATIONS = listOf(
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10
)
