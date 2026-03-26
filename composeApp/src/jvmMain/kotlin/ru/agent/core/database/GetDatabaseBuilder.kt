package ru.agent.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), dbFileName)
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    )
        .addMigrations(*ALL_MIGRATIONS.toTypedArray())
        // Для development: пересоздать БД если схема не совпадает
        .fallbackToDestructiveMigration(dropAllTables = true)
}
