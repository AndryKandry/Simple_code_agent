package ru.agent.core.platform

import java.io.File

/**
 * Android implementation of platform-specific functions.
 */

actual fun getWorkingDirectory(): String {
    var currentDir = File(System.getProperty("user.dir")).absoluteFile

    // Go up maximum 10 levels to find project root
    // Project root is identified by settings.gradle.kts (only exists at root, not in submodules)
    var attempts = 0
    while (attempts < 10 && currentDir.parentFile != null) {
        val settingsGradle = File(currentDir, "settings.gradle.kts")

        // settings.gradle.kts only exists at project root, not in submodules
        if (settingsGradle.exists()) {
            return currentDir.absolutePath
        }

        currentDir = currentDir.parentFile!!
        attempts++
    }

    // Fallback to current directory if project root not found
    return System.getProperty("user.dir") ?: System.getProperty("user.home") ?: "."
}
