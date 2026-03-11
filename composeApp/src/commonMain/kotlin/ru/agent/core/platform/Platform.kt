package ru.agent.core.platform

/**
 * Platform-specific functions for Kotlin Multiplatform.
 */

/**
 * Get the current working directory.
 * On JVM, returns System.getProperty("user.dir")
 */
expect fun getWorkingDirectory(): String
