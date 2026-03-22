package ru.agent.features.rag.domain.service

import co.touchlab.kermit.Logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.agent.features.rag.domain.model.RagMetrics
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Service for collecting and logging RAG pipeline metrics.
 *
 * Metrics are logged in JSON format for analysis and monitoring.
 */
interface RagMetricsService {

    /**
     * Record and log RAG search metrics.
     *
     * @param metrics Metrics to record
     */
    fun recordMetrics(metrics: RagMetrics)

    /**
     * Get metrics log file path.
     *
     * @return Path to metrics log file
     */
    fun getMetricsLogPath(): String
}

/**
 * Implementation of RagMetricsService that logs to file and console.
 *
 * @property logDirectory Directory to store metrics logs
 * @property enableFileLogging Whether to log to file
 * @property enableConsoleLogging Whether to log to console
 */
class RagMetricsServiceImpl(
    private val logDirectory: String = System.getProperty("user.home") + "/.agent/rag_metrics",
    private val enableFileLogging: Boolean = true,
    private val enableConsoleLogging: Boolean = true
) : RagMetricsService {

    private val logger = Logger.withTag("RagMetrics")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

    init {
        // Create log directory if it doesn't exist
        if (enableFileLogging) {
            File(logDirectory).mkdirs()
        }
    }

    override fun recordMetrics(metrics: RagMetrics) {
        val jsonString = json.encodeToString(metrics)

        // Console logging
        if (enableConsoleLogging) {
            logger.i { metrics.toSummary() }
            logger.d { "Full metrics: $jsonString" }
        }

        // File logging
        if (enableFileLogging) {
            try {
                val timestamp = LocalDateTime.now().format(dateFormatter)
                val logFile = File(logDirectory, "rag_metrics_$timestamp.json")
                logFile.writeText(jsonString)

                logger.d { "Metrics saved to: ${logFile.absolutePath}" }
            } catch (e: Exception) {
                logger.e { "Failed to write metrics to file: ${e.message}" }
            }
        }
    }

    override fun getMetricsLogPath(): String {
        return logDirectory
    }
}
