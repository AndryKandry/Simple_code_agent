package ru.agent.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSHomeDirectory

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = NSHomeDirectory() + "/$dbFileName"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath
    )
        // ВНИМАНИЕ: Миграции для iOS пока не реализованы.
        // Используется fallbackToDestructiveMigration для разработки.
        // При выпуске в production нужно добавить миграции с использованием
        // SQLiteConnection API (как в jvmMain).
        .fallbackToDestructiveMigration(dropAllTables = true)
}