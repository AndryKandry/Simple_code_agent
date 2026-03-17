package ru.agent.mcp.orchestration

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import ru.agent.features.chat.data.remote.DeepSeekApi
import ru.agent.features.chat.data.remote.dto.MessageDto
import ru.agent.features.chat.data.remote.dto.ToolDefinitionDto

/**
 * LLM-driven planning service for creating execution plans.
 *
 * Uses DeepSeek API to analyze user requests and create
 * structured execution plans with dependencies.
 *
 * @property httpClient HTTP client for API calls
 * @property apiKey DeepSeek API key
 */
class PlanningService(
    private val httpClient: HttpClient,
    private val apiKey: String
) {

    private val logger = Logger.withTag("PlanningService")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    /**
     * Create execution plan from planning request.
     *
     * @param request Planning request with user message and context
     * @return Result containing planning response or error
     */
    suspend fun createPlan(request: PlanningRequest): Result<PlanningResponse> {
        logger.i { "Creating plan for message: ${request.userMessage.take(50)}..." }

        return try {
            // Build prompt for planning
            val prompt = buildPlanningPrompt(request)
            logger.d { "Planning prompt built (${prompt.length} chars)" }

            // Call LLM API
            val apiResponse = callPlanningApi(prompt)

            apiResponse.fold(
                onSuccess = { responseText ->
                    // Parse response
                    val planningResponse = parsePlanResponse(responseText)
                    logger.i { "Plan created: ${planningResponse.steps.size} steps" }
                    Result.success(planningResponse)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to call planning API" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.e(e) { "Error creating plan" }
            Result.failure(e)
        }
    }

    /**
     * Build planning prompt from request.
     *
     * Creates a structured prompt that instructs the LLM to:
     * 1. Analyze the user request
     * 2. Identify required tools
     * 3. Determine execution order and dependencies
     * 4. Output structured JSON
     *
     * @param request Planning request
     * @return Formatted prompt string
     */
    fun buildPlanningPrompt(request: PlanningRequest): String {
        val toolsDescription = formatToolsForPrompt(request.availableTools)

        return """
            You are a planning assistant for an AI agent system. Your task is to create an execution plan for the user's request.

            # Available Tools

            $toolsDescription

            # Context

            - Working Directory: ${request.context.workingDirectory}
            - Session ID: ${request.context.sessionId}
            - Available Servers: ${request.context.availableServers.joinToString { "${it.name} (${it.toolCount} tools)" }}

            # User Request

            ${request.userMessage}

            # Task

            Create an execution plan in JSON format with the following structure:

            ```json
            {
              "steps": [
                {
                  "tool": "tool_name_here",
                  "arguments": {
                    "arg1": "value1",
                    "arg2": "value2"
                  },
                  "dependsOn": []
                }
              ],
              "canParallelize": true,
              "reasoning": "Explanation of the plan"
            }
            ```

            ## Rules

            1. Each step must use a tool from the available tools list
            2. `dependsOn` contains indices of steps this step depends on (0-based)
            3. Steps with no dependencies can run in parallel
            4. Set `canParallelize` to true if any steps can run in parallel
            5. Provide brief reasoning for the plan
            6. Use actual file paths from the working directory context

            ## Examples

            Request: "Read the build.gradle.kts file and check if it compiles"
            ```json
            {
              "steps": [
                {
                  "tool": "filesystem_read_file",
                  "arguments": {"path": "${request.context.workingDirectory}/build.gradle.kts"},
                  "dependsOn": []
                },
                {
                  "tool": "terminal_execute_command",
                  "arguments": {"command": "./gradlew build", "working_dir": "${request.context.workingDirectory}"},
                  "dependsOn": [0]
                }
              ],
              "canParallelize": false,
              "reasoning": "Build check depends on reading the file first"
            }
            ```

            Respond ONLY with the JSON plan, no additional text.
        """.trimIndent()
    }

    /**
     * Parse LLM response into planning response.
     *
     * @param responseText Raw response text from LLM
     * @return Parsed planning response
     */
    fun parsePlanResponse(responseText: String): PlanningResponse {
        logger.d { "Parsing plan response" }

        return try {
            // Extract JSON from response (handle markdown code blocks)
            val jsonText = extractJson(responseText)

            // Parse JSON
            val parsedResponse = json.decodeFromString<PlanningResponseDto>(jsonText)

            // Convert to domain model
            PlanningResponse(
                steps = parsedResponse.steps.map { step ->
                    PlannedStep(
                        tool = step.tool,
                        arguments = step.arguments.mapValues { (_, jsonElement) ->
                            jsonElement.toAny()
                        },
                        dependsOn = step.dependsOn
                    )
                },
                canParallelize = parsedResponse.canParallelize,
                reasoning = parsedResponse.reasoning
            )
        } catch (e: Exception) {
            logger.w(e) { "Failed to parse plan response, returning empty plan" }
            PlanningResponse(
                steps = emptyList(),
                canParallelize = false,
                reasoning = "Failed to parse LLM response: ${e.message}"
            )
        }
    }

    // === Private Helpers ===

    /**
     * Call DeepSeek API for planning.
     *
     * @param prompt Planning prompt
     * @return Result containing response text or error
     */
    private suspend fun callPlanningApi(prompt: String): Result<String> {
        return try {
            withTimeout(60_000L) {  // 60 seconds timeout
                val request = PlanningApiRequest(
                    model = "deepseek-chat",
                    messages = listOf(
                        MessageDto(
                            role = "system",
                            content = "You are a planning assistant. Respond only with valid JSON."
                        ),
                        MessageDto(
                            role = "user",
                            content = prompt
                        )
                    ),
                    temperature = 0.3,
                    maxTokens = 2000
                )

                val response = httpClient.post("${DeepSeekApi.BASE_URL}/chat/completions") {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(request))
                }

                if (!response.status.isSuccess()) {
                    val errorBody = response.bodyAsText()
                    logger.e { "Planning API error: ${response.status} - $errorBody" }
                    return@withTimeout Result.failure(Exception("API error: ${response.status}"))
                }

                val apiResponse = json.decodeFromString<PlanningApiResponse>(response.bodyAsText())
                val content = apiResponse.choices.firstOrNull()?.message?.content ?: ""

                Result.success(content)
            }
        } catch (e: TimeoutCancellationException) {
            logger.w { "Planning API timeout" }
            Result.failure(Exception("Planning API timeout after 60 seconds"))
        } catch (e: Exception) {
            logger.e(e) { "Error calling planning API" }
            Result.failure(e)
        }
    }

    /**
     * Format tools list for prompt.
     *
     * @param tools List of tool definitions
     * @return Formatted string for prompt
     */
    private fun formatToolsForPrompt(tools: List<ToolDefinitionDto>): String {
        return tools.joinToString("\n") { tool ->
            val params = tool.function.parameters.jsonObject
            val paramsStr = if (params.isNotEmpty()) {
                params.keys.joinToString(", ")
            } else {
                "no parameters"
            }
            "- ${tool.function.name}: ${tool.function.description.take(100)} [$paramsStr]"
        }
    }

    /**
     * Extract JSON from response text.
     *
     * Handles:
     * - Plain JSON
     * - JSON in markdown code blocks
     * - JSON with surrounding text
     *
     * @param text Response text
     * @return Extracted JSON string
     */
    private fun extractJson(text: String): String {
        val trimmed = text.trim()

        // Try to extract from markdown code block
        val codeBlockMatch = CODE_BLOCK_REGEX.find(trimmed)
        if (codeBlockMatch != null) {
            return codeBlockMatch.groupValues[2].trim()
        }

        // Try to find JSON object
        val jsonMatch = JSON_OBJECT_REGEX.find(trimmed)
        if (jsonMatch != null) {
            return jsonMatch.value
        }

        // Return as-is
        return trimmed
    }

    companion object {
        private val CODE_BLOCK_REGEX = Regex("```(json)?\\s*\\n?([\\s\\S]*?)\\n?```")
        private val JSON_OBJECT_REGEX = Regex("\\{[\\s\\S]*\\}")

        /**
         * Convert JsonElement to Kotlin primitive type.
         */
        private fun JsonElement.toAny(): Any? = when (this) {
            is JsonPrimitive -> when {
                this.isString -> this.content
                this.content == "true" -> true
                this.content == "false" -> false
                this.content.contains(".") -> this.content.toDoubleOrNull()
                else -> this.content.toLongOrNull() ?: this.content
            }
            is JsonObject -> this.mapValues { (_, value) -> value.toAny() }
            is kotlinx.serialization.json.JsonArray -> this.map { it.toAny() }
        }
    }
}

// === DTOs for Planning API ===

@Serializable
private data class PlanningApiRequest(
    val model: String,
    val messages: List<MessageDto>,
    val temperature: Double = 0.3,
    @SerialName("max_tokens")
    val maxTokens: Int = 2000
)

@Serializable
private data class PlanningApiResponse(
    val id: String,
    val model: String = "",
    val choices: List<PlanningChoice>
)

@Serializable
private data class PlanningChoice(
    val index: Int,
    val message: PlanningMessage,
    @SerialName("finish_reason")
    val finishReason: String = ""
)

@Serializable
private data class PlanningMessage(
    val role: String,
    val content: String
)

@Serializable
private data class PlanningResponseDto(
    val steps: List<PlannedStepDto>,
    val canParallelize: Boolean = false,
    val reasoning: String = ""
)

@Serializable
private data class PlannedStepDto(
    val tool: String,
    val arguments: Map<String, JsonElement> = emptyMap(),
    val dependsOn: List<Int> = emptyList()
)
