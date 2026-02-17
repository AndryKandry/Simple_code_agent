package ru.agent.core.di

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Manages application cleanup by closing resources like HttpClient
 * Should be called when the application is shutting down
 */
object ApplicationCloser : KoinComponent {

    private val httpClient: HttpClient by inject()
    private val logger = Logger.withTag("ApplicationCloser")

    /**
     * Closes all managed resources
     * Call this method when the application is closing
     */
    fun closeAll() {
        logger.i { "Closing application resources..." }
        try {
            httpClient.close()
            logger.i { "HttpClient closed successfully" }
        } catch (e: Exception) {
            logger.e(throwable = e) { "Error closing HttpClient" }
        }
    }
}
