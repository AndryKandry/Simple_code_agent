package ru.agent.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

// TODO: fallbackToDestructiveMigration только для разработки. Удалить в будущем
fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val applicationContext = context.applicationContext
    val databaseFile = applicationContext.getDatabasePath(dbFileName)

    return Room.databaseBuilder<AppDatabase>(
        context = applicationContext,
        name = databaseFile.absolutePath
    )
        // ВНИМАНИЕ: fallbackToDestructiveMigration используется только для разработки.
        // При изменении схемы базы данных старая версия будет полностью удалена и создана новая.
        // ПЕРЕД ВЫПУСКОМ В PRODUCTION необходимо заменить на реальные миграции с использованием
        // .addMigrations(Migration13To14(), ...) для сохранения данных пользователей.
        .fallbackToDestructiveMigration()
}
