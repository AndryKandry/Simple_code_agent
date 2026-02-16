package ru.agent.core.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.core.database.getDatabaseBuilder
import ru.agent.core.database.getRoomDatabase

val desktopDatabaseModule = module {
    single<AppDatabase> {
        getRoomDatabase(
            getDatabaseBuilder()
        )
    }
}

actual val platformModule: Module = desktopDatabaseModule