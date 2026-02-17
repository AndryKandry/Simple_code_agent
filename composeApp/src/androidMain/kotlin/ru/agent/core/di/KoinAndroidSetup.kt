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

actual fun getDeepSeekApiKey(): String {
    // In production, read from BuildConfig or SharedPreferences
    throw IllegalStateException(
        "DeepSeek API key is not configured for Android. " +
        "Please implement API key retrieval from BuildConfig or SharedPreferences. " +
        "Get your API key at: https://platform.deepseek.com/api_keys"
    )
}