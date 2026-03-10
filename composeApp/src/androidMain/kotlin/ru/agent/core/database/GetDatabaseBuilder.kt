package ru.agent.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val applicationContext = context.applicationContext
    val databaseFile = applicationContext.getDatabasePath(dbFileName)

    return Room.databaseBuilder<AppDatabase>(
        context = applicationContext,
        name = databaseFile.absolutePath
    )
        // Миграции для сохранения данных пользователей при обновлении схемы БД
        .addMigrations(*ALL_MIGRATIONS_ANDROID.toTypedArray())
}
