package ru.agent.mcp.server

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.*
import java.io.File

/**
 * MCP Server for filesystem operations.
 *
 * Provides tools for:
 * - read_file: Read file contents
 * - write_file: Write content to file
 * - list_directory: List directory contents
 * - search_files: Search files by pattern
 * - delete_file: Delete file (requires confirmation)
 *
 * Security:
 * - Only paths within allowed roots are accessible
 * - Directory deletion is prohibited
 * - File deletion requires confirmation
 */
class FilesystemMcpServer(
    private val allowedRoots: List<String> = listOf(System.getProperty("user.dir"))
) {
    private val server = Server(
        serverInfo = Implementation(
            name = "filesystem-mcp-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
            ),
        )
    )

    init {
        registerTools()
    }

    private fun registerTools() {
        // read_file
        server.addTool(
            name = "read_file",
            description = "Read the contents of a file",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("path", buildJsonObject {
                        put("type", "string")
                        put("description", "Path to the file to read")
                    })
                },
                required = listOf("path")
            )
        ) { request ->
            val path = request.arguments?.get("path")?.jsonPrimitive?.content ?: ""
            val result = readFile(path)
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // write_file
        server.addTool(
            name = "write_file",
            description = "Write content to a file",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("path", buildJsonObject {
                        put("type", "string")
                        put("description", "Path to the file to write")
                    })
                    put("content", buildJsonObject {
                        put("type", "string")
                        put("description", "Content to write to the file")
                    })
                },
                required = listOf("path", "content")
            )
        ) { request ->
            val path = request.arguments?.get("path")?.jsonPrimitive?.content ?: ""
            val content = request.arguments?.get("content")?.jsonPrimitive?.content ?: ""
            val result = writeFile(path, content)
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // list_directory
        server.addTool(
            name = "list_directory",
            description = "List contents of a directory",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("path", buildJsonObject {
                        put("type", "string")
                        put("description", "Path to the directory")
                    })
                },
                required = listOf("path")
            )
        ) { request ->
            val path = request.arguments?.get("path")?.jsonPrimitive?.content ?: ""
            val result = listDirectory(path)
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // search_files
        server.addTool(
            name = "search_files",
            description = "Search for files matching a pattern",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("path", buildJsonObject {
                        put("type", "string")
                        put("description", "Root path to search from")
                    })
                    put("pattern", buildJsonObject {
                        put("type", "string")
                        put("description", "Glob pattern to match files")
                    })
                },
                required = listOf("path", "pattern")
            )
        ) { request ->
            val path = request.arguments?.get("path")?.jsonPrimitive?.content ?: ""
            val pattern = request.arguments?.get("pattern")?.jsonPrimitive?.content ?: "*"
            val result = searchFiles(path, pattern)
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // delete_file - WITH CONFIRMATION!
        server.addTool(
            name = "delete_file",
            description = "Delete a file (REQUIRES CONFIRMATION)",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("path", buildJsonObject {
                        put("type", "string")
                        put("description", "Path to the file to delete")
                    })
                    put("confirm", buildJsonObject {
                        put("type", "boolean")
                        put("description", "Must be true to confirm deletion")
                    })
                },
                required = listOf("path", "confirm")
            )
        ) { request ->
            val path = request.arguments?.get("path")?.jsonPrimitive?.content ?: ""
            val confirm = request.arguments?.get("confirm")?.jsonPrimitive?.booleanOrNull ?: false
            val result = deleteFile(path, confirm)
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // create_directory
        server.addTool(
            name = "create_directory",
            description = "Create a new directory",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("path", buildJsonObject {
                        put("type", "string")
                        put("description", "Path to the directory to create")
                    })
                },
                required = listOf("path")
            )
        ) { request ->
            val path = request.arguments?.get("path")?.jsonPrimitive?.content ?: ""
            val result = createDirectory(path)
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // file_exists
        server.addTool(
            name = "file_exists",
            description = "Check if a file or directory exists",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("path", buildJsonObject {
                        put("type", "string")
                        put("description", "Path to check")
                    })
                },
                required = listOf("path")
            )
        ) { request ->
            val path = request.arguments?.get("path")?.jsonPrimitive?.content ?: ""
            val result = fileExists(path)
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // copy_file
        server.addTool(
            name = "copy_file",
            description = "Copy a file to a new location",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("source", buildJsonObject {
                        put("type", "string")
                        put("description", "Source file path")
                    })
                    put("destination", buildJsonObject {
                        put("type", "string")
                        put("description", "Destination file path")
                    })
                },
                required = listOf("source", "destination")
            )
        ) { request ->
            val source = request.arguments?.get("source")?.jsonPrimitive?.content ?: ""
            val destination = request.arguments?.get("destination")?.jsonPrimitive?.content ?: ""
            val result = copyFile(source, destination)
            CallToolResult(content = listOf(TextContent(text = result)))
        }
    }

    // === Path Validation ===

    private fun validatePath(path: String): Boolean {
        return try {
            val canonicalPath = File(path).canonicalPath
            allowedRoots.any { root ->
                canonicalPath.startsWith(File(root).canonicalPath)
            }
        } catch (e: Exception) {
            false
        }
    }

    // === Tool Implementations ===

    private fun readFile(path: String): String {
        if (!validatePath(path)) {
        val canonicalPath = try { File(path).canonicalPath } catch (e: Exception) { path }
        return "Error: Access denied. Path outside allowed roots.\n" +
               "Requested: $path\n" +
               "Resolved to: $canonicalPath\n" +
               "Allowed roots: ${allowedRoots.joinToString(", ")}\n" +
               "HINT: Use the FULL ABSOLUTE PATH shown in WORKING DIRECTORY from the system prompt."
        }
        val file = File(path)
        if (!file.exists()) {
            return "Error: File not found: $path"
        }
        if (!file.isFile) {
            return "Error: Not a file: $path"
        }
        return try {
            file.readText()
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }

    private fun writeFile(path: String, content: String): String {
        if (!validatePath(path)) {
            return "Error: Access denied. Path outside allowed roots."
        }
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            "Successfully wrote ${content.length} characters to $path"
        } catch (e: Exception) {
            "Error writing file: ${e.message}"
        }
    }

    private fun listDirectory(path: String): String {
        if (!validatePath(path)) {
            return "Error: Access denied. Path outside allowed roots."
        }
        val dir = File(path)
        if (!dir.exists()) {
            return "Error: Directory not found: $path"
        }
        if (!dir.isDirectory) {
            return "Error: Not a directory: $path"
        }
        val entries = dir.listFiles()?.map { file ->
            val type = if (file.isDirectory) "[DIR]" else "[FILE]"
            val size = if (file.isFile) " (${formatFileSize(file.length())})" else ""
            "$type ${file.name}$size"
        }?.sorted() ?: emptyList()
        return if (entries.isEmpty()) "Directory is empty"
               else entries.joinToString("\n")
    }

    private fun searchFiles(path: String, pattern: String): String {
        if (!validatePath(path)) {
            return "Error: Access denied. Path outside allowed roots."
        }
        val root = File(path)
        if (!root.exists()) {
            return "Error: Directory not found: $path"
        }
        if (!root.isDirectory) {
            return "Error: Not a directory: $path"
        }

        val results = mutableListOf<String>()
        val regexPattern = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
            .toRegex(RegexOption.IGNORE_CASE)

        root.walkTopDown()
            .filter { it.isFile }
            .filter { regexPattern.containsMatchIn(it.name) }
            .forEach { results.add(it.absolutePath) }

        return if (results.isEmpty()) "No files found matching pattern: $pattern"
               else results.joinToString("\n")
    }

    private fun deleteFile(path: String, confirm: Boolean): String {
        if (!confirm) {
            return "Error: Deletion requires confirmation. Set 'confirm' to true."
        }
        if (!validatePath(path)) {
            return "Error: Access denied. Path outside allowed roots."
        }
        val file = File(path)
        if (!file.exists()) {
            return "Error: File not found: $path"
        }
        if (file.isDirectory) {
            return "Error: Directory deletion is not allowed for safety reasons."
        }
        return try {
            if (file.delete()) "Successfully deleted: $path"
            else "Error: Failed to delete: $path"
        } catch (e: Exception) {
            "Error deleting file: ${e.message}"
        }
    }

    private fun createDirectory(path: String): String {
        if (!validatePath(path)) {
            return "Error: Access denied. Path outside allowed roots."
        }
        val dir = File(path)
        if (dir.exists()) {
            return "Error: Path already exists: $path"
        }
        return try {
            if (dir.mkdirs()) "Successfully created directory: $path"
            else "Error: Failed to create directory: $path"
        } catch (e: Exception) {
            "Error creating directory: ${e.message}"
        }
    }

    private fun fileExists(path: String): String {
        if (!validatePath(path)) {
            return "Error: Access denied. Path outside allowed roots."
        }
        val file = File(path)
        return when {
            !file.exists() -> "Path does not exist: $path"
            file.isDirectory -> "Directory exists: $path"
            file.isFile -> "File exists: $path (${formatFileSize(file.length())})"
            else -> "Path exists: $path"
        }
    }

    private fun copyFile(source: String, destination: String): String {
        if (!validatePath(source) || !validatePath(destination)) {
            return "Error: Access denied. Source or destination outside allowed roots."
        }
        val sourceFile = File(source)
        val destFile = File(destination)

        if (!sourceFile.exists()) {
            return "Error: Source file not found: $source"
        }
        if (!sourceFile.isFile) {
            return "Error: Source is not a file: $source"
        }

        return try {
            destFile.parentFile?.mkdirs()
            sourceFile.copyTo(destFile, overwrite = true)
            "Successfully copied ${sourceFile.name} to $destination"
        } catch (e: Exception) {
            "Error copying file: ${e.message}"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    // === Direct API (non-MCP) ===

    fun readFileSync(path: String): Result<String> = Result.success(readFile(path))

    fun writeFileSync(path: String, content: String): Result<String> = Result.success(writeFile(path, content))

    fun listDirectorySync(path: String): Result<String> = Result.success(listDirectory(path))

    fun searchFilesSync(path: String, pattern: String): Result<String> = Result.success(searchFiles(path, pattern))

    fun deleteFileSync(path: String, confirm: Boolean = false): Result<String> =
        Result.success(deleteFile(path, confirm))

    fun createDirectorySync(path: String): Result<String> =
        Result.success(createDirectory(path))

    fun fileExistsSync(path: String): Result<String> =
        Result.success(fileExists(path))

    fun copyFileSync(source: String, destination: String): Result<String> =
        Result.success(copyFile(source, destination))

    fun getAvailableTools(): List<String> = listOf(
        "read_file", "write_file", "list_directory", "search_files",
        "delete_file", "create_directory", "file_exists", "copy_file"
    )

    /**
     * Start the MCP server using STDIO transport.
     */
    fun start() = runBlocking {
        val transport = StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered()
        )
        server.connect(transport)
        // Keep running until closed
        transport.onClose {
            // Connection closed
        }
    }
}

fun main() {
    FilesystemMcpServer().start()
}
