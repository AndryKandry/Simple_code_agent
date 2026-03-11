package ru.agent.features.chat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Tool definition for DeepSeek API function calling.
 *
 * DeepSeek uses OpenAI-compatible tool format.
 * IMPORTANT: type field is hardcoded to "function"
 *
 * Example:
 * ```json
 * {
 *   "type": "function",
 *   "function": {
 *     "name": "filesystem_read_file",
 *     "description": "Read file contents",
 *     "parameters": {
 *       "type": "object",
 *       "properties": {
 *         "path": {"type": "string", "description": "File path"}
 *       },
 *       "required": ["path"]
 *     }
 *   }
 * }
 * ```
 */
@Serializable
data class ToolDefinitionDto(
    @SerialName("type")
    val type: String = "function",
    @SerialName("function")
    val function: FunctionDefinitionDto
)

@Serializable
data class FunctionDefinitionDto(
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String = "",
    @SerialName("parameters")
    val parameters: JsonObject = JsonObject(emptyMap())
)

/**
 * Tool call from API response.
 *
 * When LLM decides to use a tool, it returns tool_calls in the message.
 *
 * Example:
 * ```json
 * {
 *   "id": "call_abc123",
 *   "type": "function",
 *   "function": {
 *     "name": "filesystem:read_file",
 *     "arguments": "{\"path\": \"build.gradle.kts\"}"
 *   }
 * }
 * ```
 */
@Serializable
data class ToolCallDto(
    @SerialName("id")
    val id: String,
    @SerialName("type")
    val type: String = "function",
    @SerialName("function")
    val function: FunctionCallDto
)

@Serializable
data class FunctionCallDto(
    @SerialName("name")
    val name: String,
    @SerialName("arguments")
    val arguments: String  // JSON string with arguments
)
