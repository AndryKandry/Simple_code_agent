package ru.agent.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSHomeDirectory

fun getDatabaseBuilder() : RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = NSHomeDirectory() + "/$dbFileName"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath,
//        factory = {
//            AppDatabase::class.instantiateImpl()
//        }
    )
        // ВНИМАНИЕ: fallbackToDestructiveMigration используется только для разработки.
        // При изменении схемы базы данных старая версия будет полностью удалена и создана новая.
        // ПЕРЕД ВЫПУСКОМ В PRODUCTION необходимо заменить на реальные миграции с использованием
        // .addMigrations(Migration13To14(), ...) для сохранения данных пользователей.
        .fallbackToDestructiveMigration(dropAllTables = true)
}