package ru.agent.features.rag.collector

import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.pathString

/**
 * Information about a collected file.
 *
 * @property path Full path to the file
 * @property relativePath Relative path from project root
 * @property extension File extension
 * @property language Detected programming language
 */
data class FileInfo(
    val path: Path,
    val relativePath: String,
    val extension: String,
    val language: String
)

/**
 * Collects files from a git repository using `git ls-files`.
 * Filters files based on extension and directory exclusions.
 */
class GitFilesCollector(private val projectPath: Path) {

    /**
     * Collects all relevant files from the git repository.
     *
     * @return List of FileInfo for files that should be indexed
     */
    suspend fun collectFiles(): List<FileInfo> {
        val gitFiles = executeGitLsFiles()

        return gitFiles
            .filter { shouldInclude(it) }
            .map { relativePath ->
                val fullPath = projectPath.resolve(relativePath)
                val extension = fullPath.extension
                FileInfo(
                    path = fullPath,
                    relativePath = relativePath,
                    extension = extension,
                    language = detectLanguage(extension)
                )
            }
            .filter { !it.path.isDirectory() }
    }

    /**
     * Executes `git ls-files` command and returns list of tracked files.
     *
     * @throws GitException if git command fails or git is not available
     */
    private fun executeGitLsFiles(): List<String> {
        return try {
            val process = ProcessBuilder("git", "ls-files")
                .directory(projectPath.toFile())
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readLines()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                val error = output.joinToString("\n")
                throw GitException("Git command failed with exit code $exitCode: $error")
            }

            output.filter { it.isNotBlank() }
        } catch (e: IOException) {
            throw GitException("Git not found. Ensure git is installed and accessible.", e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw GitException("Git command was interrupted", e)
        }
    }

    /**
     * Determines if a file should be included in the index.
     */
    private fun shouldInclude(path: String): Boolean {
        // Check excluded directories
        val pathParts = path.split("/")
        if (pathParts.any { it in EXCLUDED_DIRECTORIES }) {
            return false
        }

        // Check excluded patterns
        val fileName = path.substringAfterLast("/")
        if (EXCLUDED_PATTERNS.any { pattern ->
            fileName.contains(pattern.replace("*", "").toRegex())
        }) {
            return false
        }

        // Check file extension
        val extension = path.substringAfterLast(".", "")
        return extension in INCLUDED_EXTENSIONS
    }

    /**
     * Detects programming language from file extension.
     */
    private fun detectLanguage(extension: String): String {
        return LANGUAGE_MAP[extension] ?: "text"
    }

    companion object {
        /**
         * File extensions to include in indexing.
         */
        val INCLUDED_EXTENSIONS = setOf(
            // Kotlin
            "kt", "kts",
            // Java
            "java",
            // Python
            "py",
            // JavaScript/TypeScript
            "js", "ts", "jsx", "tsx", "mjs", "cjs",
            // Go
            "go",
            // Rust
            "rs",
            // C/C++
            "c", "cpp", "cc", "cxx", "h", "hpp",
            // Documentation
            "md", "txt", "rst", "adoc",
            // Config
            "json", "yaml", "yml", "xml", "toml", "ini",
            // Build
            "gradle", "gradle.kts", "sb",
            // SQL
            "sql",
            // Shell
            "sh", "bash", "zsh",
            // Other
            "proto", "graphql", "gql", "vue", "svelte"
        )

        /**
         * Directories to exclude from indexing.
         */
        val EXCLUDED_DIRECTORIES = setOf(
            "build", ".gradle", ".idea", ".git", ".kotlin",
            "node_modules", "target", "out", "dist", "bin",
            ".venv", "venv", "__pycache__", ".tox",
            "vendor", "Pods", ".flutter-plugins",
            ".mvn", "gradle"
        )

        /**
         * File patterns to exclude.
         */
        val EXCLUDED_PATTERNS = setOf(
            "*.min.js",
            "*.min.css",
            "*.generated.*",
            "*.pb.*",
            "*_generated.*"
        )

        /**
         * Mapping of file extensions to language names.
         */
        val LANGUAGE_MAP = mapOf(
            "kt" to "kotlin",
            "kts" to "kotlin",
            "java" to "java",
            "py" to "python",
            "js" to "javascript",
            "mjs" to "javascript",
            "cjs" to "javascript",
            "ts" to "typescript",
            "jsx" to "javascript",
            "tsx" to "typescript",
            "go" to "go",
            "rs" to "rust",
            "c" to "c",
            "cpp" to "cpp",
            "cc" to "cpp",
            "cxx" to "cpp",
            "h" to "c",
            "hpp" to "cpp",
            "md" to "markdown",
            "txt" to "text",
            "rst" to "rst",
            "adoc" to "asciidoc",
            "json" to "json",
            "yaml" to "yaml",
            "yml" to "yaml",
            "xml" to "xml",
            "toml" to "toml",
            "ini" to "ini",
            "gradle" to "gradle",
            "sql" to "sql",
            "sh" to "shell",
            "bash" to "shell",
            "zsh" to "shell",
            "proto" to "protobuf",
            "graphql" to "graphql",
            "gql" to "graphql",
            "vue" to "vue",
            "svelte" to "svelte"
        )
    }
}

/**
 * Exception thrown when git operations fail.
 *
 * @property message Error message describing what went wrong
 * @property cause Original exception that caused this error
 */
class GitException(
    override val message: String?,
    override val cause: Throwable? = null
) : Exception(message, cause)
