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
 * Список всех миграций для Android.
 */
val ALL_MIGRATIONS_ANDROID = listOf(
    MIGRATION_6_7_ANDROID,
    MIGRATION_7_8_ANDROID
)
