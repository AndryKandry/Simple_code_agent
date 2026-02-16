package ru.agent

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import ru.agent.core.di.initKoin

class MobileApplication : Application(), KoinComponent {

    override fun onCreate() {
        super.onCreate()

        initDI()
    }

    private fun initDI() {
        initKoin(
            setupContext = { androidContext(this@MobileApplication) }
        )
    }

}
