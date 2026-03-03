package ru.agent.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDatabaseBuilder() : RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), dbFileName)
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    )
        // ВНИМАНИЕ: fallbackToDestructiveMigration используется только для разработки.
        // При изменении схемы базы данных старая версия будет полностью удалена и создана новая.
        // ПЕРЕД ВЫПУСКОМ В PRODUCTION необходимо заменить на реальные миграции с использованием
        // .addMigrations(Migration13To14(), ...) для сохранения данных пользователей.
        .fallbackToDestructiveMigration(dropAllTables = true)
}
