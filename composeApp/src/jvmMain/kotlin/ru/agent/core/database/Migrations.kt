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
 * Список всех миграций для добавления в Room database builder.
 */
val ALL_MIGRATIONS = listOf(
    MIGRATION_6_7,
    MIGRATION_7_8
)
