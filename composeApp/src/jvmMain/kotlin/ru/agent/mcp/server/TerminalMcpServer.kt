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
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * MCP Server for terminal command execution.
 *
 * Provides tools for:
 * - execute_command: Execute terminal commands (safe commands only)
 * - run_shell_script: Run shell script files
 * - check_command_available: Check if command exists on system
 *
 * Security:
 * - Command whitelist: only allowed commands
 * - Dangerous pattern blacklist: blocks rm -rf, sudo, etc.
 * - Command timeout: prevents hanging commands
 * - Project-only scripts: only run scripts from project directory
 */
class TerminalMcpServer(
    private val projectRoot: String = System.getProperty("user.dir")
) {
    private val server = Server(
        serverInfo = Implementation(
            name = "terminal-mcp-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
            ),
        )
    )

    // Whitelist of allowed commands
    private val allowedCommands = setOf(
        // Navigation and file inspection
        "ls", "pwd", "cat", "find", "grep", "head", "tail", "wc", "less", "more",
        "tree", "file", "stat", "du", "df",
        // Build tools
        "gradlew", "gradle", "mvn", "npm", "yarn", "pnpm", "cargo", "go", "make",
        "cmake", "ant", "sbt",
        // Languages and interpreters
        "python", "python3", "pip", "pip3", "node", "ruby", "perl", "php",
        "java", "javac", "kotlin", "kotlinc", "scala", "swift", "rustc",
        // Version control
        "git", "svn", "hg",
        // Utility
        "echo", "which", "whoami", "date", "uname", "env", "printenv",
        "hostname", "id", "uptime", "ps", "top", "htop",
        // Network (safe)
        "curl", "wget", "ping", "nc", "telnet",
        // Docker (with restrictions)
        "docker",
        // Kubernetes (with restrictions)
        "kubectl"
    )

    // Blacklist of dangerous patterns
    private val dangerousPatterns = listOf(
        // Filesystem destruction
        "rm -rf", "rm -r", "rm -f", "rmdir", "shred",
        // Privilege escalation
        "sudo", "su ", "doas", "pkexec",
        // Permission changes
        "chmod 777", "chmod -R 777", "chown -R",
        // System damage
        "> /dev/", "mkfs", "dd if=", "dd of=",
        // Remote code execution
        "curl | bash", "curl | sh", "wget | bash", "wget | sh",
        "curl | sudo", "wget | sudo",
        // Fork bombs
        ":(){ :|:& };:", "fork bomb",
        // Environment pollution
        "export PATH", "export LD_PRELOAD",
        // Network attacks
        "nmap -sS", "nmap -sT", "hydra", "john",
        // Docker dangerous
        "docker rm", "docker rmi", "docker system prune",
        // Kubernetes dangerous
        "kubectl delete", "kubectl drain"
    )

    init {
        registerTools()
    }

    private fun registerTools() {
        // execute_command
        server.addTool(
            name = "execute_command",
            description = "Execute a terminal command (safe commands only). Commands are validated against whitelist.",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("command", buildJsonObject {
                        put("type", "string")
                        put("description", "The command to execute")
                    })
                    put("timeout", buildJsonObject {
                        put("type", "number")
                        put("description", "Timeout in milliseconds (default: 30000, max: 120000)")
                    })
                    put("working_dir", buildJsonObject {
                        put("type", "string")
                        put("description", "Working directory (default: project root)")
                    })
                },
                required = listOf("command")
            )
        ) { request ->
            val command = request.arguments?.get("command")?.jsonPrimitive?.content ?: ""
            val timeout = request.arguments?.get("timeout")?.jsonPrimitive?.longOrNull ?: 30000L
            val workingDir = request.arguments?.get("working_dir")?.jsonPrimitive?.contentOrNull
            val result = executeCommand(command, timeout.coerceIn(1000, 120000), workingDir)
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // run_shell_script
        server.addTool(
            name = "run_shell_script",
            description = "Run a shell script file. Script must be within project directory.",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("script_path", buildJsonObject {
                        put("type", "string")
                        put("description", "Path to the shell script")
                    })
                    put("args", buildJsonObject {
                        put("type", "array")
                        put("description", "Arguments to pass to the script")
                        put("items", buildJsonObject { put("type", "string") })
                    })
                    put("timeout", buildJsonObject {
                        put("type", "number")
                        put("description", "Timeout in milliseconds (default: 60000)")
                    })
                },
                required = listOf("script_path")
            )
        ) { request ->
            val scriptPath = request.arguments?.get("script_path")?.jsonPrimitive?.content ?: ""
            val args = request.arguments?.get("args")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val timeout = request.arguments?.get("timeout")?.jsonPrimitive?.longOrNull ?: 60000L
            val result = runShellScript(scriptPath, args, timeout)
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // check_command_available
        server.addTool(
            name = "check_command_available",
            description = "Check if a command is available on the system",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("command", buildJsonObject {
                        put("type", "string")
                        put("description", "The command to check")
                    })
                },
                required = listOf("command")
            )
        ) { request ->
            val command = request.arguments?.get("command")?.jsonPrimitive?.content ?: ""
            val result = checkCommandAvailable(command)
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // list_allowed_commands
        server.addTool(
            name = "list_allowed_commands",
            description = "List all commands that are allowed to be executed",
            inputSchema = Tool.Input(
                properties = buildJsonObject {},
                required = emptyList()
            )
        ) { _ ->
            val result = listAllowedCommands()
            CallToolResult(content = listOf(TextContent(text = result)))
        }

        // get_environment_info
        server.addTool(
            name = "get_environment_info",
            description = "Get information about the execution environment",
            inputSchema = Tool.Input(
                properties = buildJsonObject {},
                required = emptyList()
            )
        ) { _ ->
            val result = getEnvironmentInfo()
            CallToolResult(content = listOf(TextContent(text = result)))
        }
    }

    // === Command Validation ===

    private fun validateCommand(command: String): Pair<Boolean, String> {
        val trimmedCommand = command.trim()

        // Check for empty command
        if (trimmedCommand.isEmpty()) {
            return Pair(false, "Error: Empty command")
        }

        // Check for dangerous patterns
        for (pattern in dangerousPatterns) {
            if (trimmedCommand.contains(pattern, ignoreCase = true)) {
                return Pair(false, "Error: Command contains dangerous pattern: $pattern")
            }
        }

        // Extract base command (first word)
        val baseCommand = trimmedCommand.split("\\s+".toRegex()).firstOrNull() ?: ""

        // Check whitelist
        if (baseCommand !in allowedCommands) {
            return Pair(false, "Error: Command '$baseCommand' is not in the allowed list.\n" +
                    "Allowed commands: ${allowedCommands.sorted().take(20).joinToString(", ")}... (use list_allowed_commands to see all)")
        }

        return Pair(true, "OK")
    }

    // === Tool Implementations ===

    private fun executeCommand(command: String, timeout: Long, workingDir: String?): String {
        val (isValid, validationMessage) = validateCommand(command)
        if (!isValid) {
            return validationMessage
        }

        // Validate working directory
        val workDir = workingDir?.let { dir ->
            val file = File(dir)
            if (!file.exists() || !file.isDirectory) {
                return "Error: Working directory not found: $dir"
            }
            if (!file.canonicalPath.startsWith(projectRoot)) {
                return "Error: Working directory must be within project: $dir"
            }
            file
        } ?: File(projectRoot)

        return try {
            val process = ProcessBuilder()
                .command("sh", "-c", command)
                .directory(workDir)
                .redirectErrorStream(true)
                .start()

            val output = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            val thread = Thread {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.appendLine(line)
                }
            }
            thread.start()

            val finished = process.waitFor(timeout, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return "Error: Command timed out after ${timeout}ms\nPartial output:\n$output"
            }

            thread.join(1000)

            val exitCode = process.exitValue()
            val header = "Exit code: $exitCode | Command: $command\n${"─".repeat(50)}\n"

            if (output.isEmpty()) {
                "$header(Command executed successfully with no output)"
            } else {
                "$header$output"
            }
        } catch (e: Exception) {
            "Error executing command: ${e.message}"
        }
    }

    private fun runShellScript(scriptPath: String, args: List<String>, timeout: Long): String {
        // Validate script path
        val scriptFile = File(scriptPath)
        if (!scriptFile.exists()) {
            return "Error: Script not found: $scriptPath"
        }
        if (!scriptFile.isFile) {
            return "Error: Not a file: $scriptPath"
        }
        if (!scriptFile.canonicalPath.startsWith(projectRoot)) {
            return "Error: Script must be within the project directory"
        }
        if (!scriptFile.canExecute()) {
            scriptFile.setExecutable(true)
        }

        // Build command with escaped arguments
        val escapedArgs = args.joinToString(" ") { arg ->
            "'" + arg.replace("'", "'\\''") + "'"
        }
        val command = "sh '$scriptPath' $escapedArgs"

        // Validate the command
        val (isValid, validationMessage) = validateCommand("sh")
        if (!isValid) {
            return validationMessage
        }

        return executeCommand(command, timeout, scriptFile.parentFile?.absolutePath)
    }

    private fun checkCommandAvailable(command: String): String {
        return try {
            val process = ProcessBuilder()
                .command("which", command)
                .directory(File(projectRoot))
                .start()
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                val path = process.inputStream.bufferedReader().readText().trim()
                val allowed = command in allowedCommands
                """
                Command: $command
                Status: Available
                Path: $path
                Allowed by whitelist: $allowed
                """.trimIndent()
            } else {
                """
                Command: $command
                Status: Not available
                Allowed by whitelist: ${command in allowedCommands}
                """.trimIndent()
            }
        } catch (e: Exception) {
            "Error checking command: ${e.message}"
        }
    }

    private fun listAllowedCommands(): String {
        val sorted = allowedCommands.sorted()
        return buildString {
            appendLine("Allowed Commands (${sorted.size} total):")
            appendLine("─".repeat(50))
            sorted.chunked(5).forEach { chunk ->
                appendLine(chunk.joinToString(", ").padEnd(60))
            }
        }
    }

    private fun getEnvironmentInfo(): String {
        return buildString {
            appendLine("Environment Information")
            appendLine("─".repeat(50))
            appendLine("Project Root: $projectRoot")
            appendLine("Current Directory: ${System.getProperty("user.dir")}")
            appendLine("User: ${System.getProperty("user.name")}")
            appendLine("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
            appendLine("Java Version: ${System.getProperty("java.version")}")
            appendLine("Shell: ${System.getenv("SHELL") ?: "unknown"}")
            appendLine("Allowed Commands: ${allowedCommands.size}")
            appendLine("Dangerous Patterns Blocked: ${dangerousPatterns.size}")
        }
    }

    // === Direct API (non-MCP) ===

    /**
     * Execute command directly (without MCP protocol)
     */
    fun executeCommandSync(
        command: String,
        timeout: Long = 30000,
        workingDir: String? = null
    ): Result<String> {
        return try {
            Result.success(executeCommand(command, timeout, workingDir))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Run shell script directly
     */
    fun runShellScriptSync(
        scriptPath: String,
        args: List<String> = emptyList(),
        timeout: Long = 60000
    ): Result<String> {
        return try {
            Result.success(runShellScript(scriptPath, args, timeout))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if command is available on system
     */
    fun checkCommandAvailableSync(command: String): Result<String> =
        Result.success(checkCommandAvailable(command))

    /**
     * Get list of allowed commands
     */
    fun listAllowedCommandsSync(): Result<String> =
        Result.success(listAllowedCommands())

    /**
     * Get environment info
     */
    fun getEnvironmentInfoSync(): Result<String> =
        Result.success(getEnvironmentInfo())

    /**
     * Check if command is allowed
     */
    fun isCommandAllowed(command: String): Boolean {
        return validateCommand(command).first
    }

    /**
     * Get list of available tools
     */
    fun getAvailableTools(): List<String> = listOf(
        "execute_command",
        "run_shell_script",
        "check_command_available",
        "list_allowed_commands",
        "get_environment_info"
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
    TerminalMcpServer().start()
}
