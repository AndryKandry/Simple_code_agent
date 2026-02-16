package ru.agent.core.di

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.core.database.getDatabaseBuilder
import ru.agent.core.database.getRoomDatabase

val androidDatabaseModule = module {
//    val applicationContext = get<Application>()
    single<AppDatabase> {
        getRoomDatabase(
            getDatabaseBuilder(
                androidApplication()//androidContext()//androidApplication()
            )
        )
    }
}

actual val platformModule: Module = androidDatabaseModule