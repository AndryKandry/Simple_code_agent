package ru.agent.features.rag.collector

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * Reads file contents from the filesystem.
 * Provides synchronous and batch file reading operations.
 */
class FileContentReader {

    /**
     * Reads the content of a single file.
     *
     * @param path Path to the file
     * @return File content as string, or empty string if file cannot be read
     */
    suspend fun readFile(path: Path): String {
        return try {
            if (path.exists() && path.isRegularFile()) {
                // Check file size before reading (skip files > 1MB)
                val fileSize = Files.size(path)
                if (fileSize > MAX_FILE_SIZE) {
                    return ""
                }
                path.readText(Charsets.UTF_8)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Reads contents of multiple files.
     *
     * @param paths List of file paths to read
     * @return Map of path to file content
     */
    suspend fun readFiles(paths: List<Path>): Map<Path, String> {
        return paths.associateWith { path ->
            readFile(path)
        }.filterValues { it.isNotEmpty() }
    }

    /**
     * Checks if a file is readable.
     *
     * @param path Path to check
     * @return true if file can be read
     */
    fun isReadable(path: Path): Boolean {
        return try {
            path.exists() &&
            path.isRegularFile() &&
            Files.size(path) <= MAX_FILE_SIZE &&
            Files.isReadable(path)
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        /**
         * Maximum file size to read (1MB)
         */
        const val MAX_FILE_SIZE: Long = 1024 * 1024
    }
}
