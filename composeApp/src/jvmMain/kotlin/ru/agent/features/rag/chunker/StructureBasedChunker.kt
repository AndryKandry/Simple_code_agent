package ru.agent.features.rag.chunker

import ru.agent.features.rag.domain.model.DocumentChunk

/**
 * Structure-based chunking strategy.
 * Splits content based on code structure (classes, functions, sections)
 * with fallback to fixed-size chunking for large sections.
 *
 * Supports:
 * - Kotlin/Java: class, interface, object, fun, //=== markers
 * - Markdown: headers (#, ##), code blocks (```), horizontal rules (---)
 * - Python: class, def
 * - JavaScript/TypeScript: class, function, const, let
 */
class StructureBasedChunker(
    private val maxSectionSize: Int = MAX_SECTION_SIZE,
    private val fallbackChunker: FixedSizeChunker = FixedSizeChunker()
) : TextChunker {

    override fun chunk(content: String, metadata: ChunkMetadata): List<DocumentChunk> {
        if (content.isBlank()) {
            return emptyList()
        }

        val patterns = getPatternsForLanguage(metadata.language)
        val sections = extractSections(content, patterns)

        return sections.flatMap { section ->
            if (section.content.length > maxSectionSize) {
                // Fallback to fixed-size chunking for large sections
                fallbackChunker.chunk(section.content, metadata).map { chunk ->
                    chunk.copy(
                        chunkId = "${chunk.chunkId}_${section.name?.sanitizeForId() ?: "section"}",
                        section = section.name,
                        startLine = section.startLine + (chunk.startLine - 1),
                        endLine = section.startLine + (chunk.endLine - 1)
                    )
                }
            } else {
                listOf(
                    DocumentChunk(
                        chunkId = generateChunkId(metadata.source, section),
                        content = section.content,
                        source = metadata.source,
                        fileName = metadata.fileName,
                        language = metadata.language,
                        startLine = section.startLine,
                        endLine = section.endLine,
                        section = section.name
                    )
                )
            }
        }
    }

    /**
     * Extracts sections from content based on language patterns.
     */
    private fun extractSections(content: String, patterns: List<StructurePattern>): List<Section> {
        val lines = content.lines()
        val sections = mutableListOf<Section>()
        var currentSectionStart = 0
        var currentSectionName: String? = null
        var currentLineNum = 1

        for ((index, line) in lines.withIndex()) {
            val matchedPattern = patterns.find { pattern ->
                pattern.regex.containsMatchIn(line)
            }

            if (matchedPattern != null) {
                // Save previous section if exists and has content
                if (currentSectionStart < index) {
                    val sectionContent = lines.subList(currentSectionStart, index).joinToString("\n")
                    if (sectionContent.isNotBlank()) {
                        sections.add(
                            Section(
                                name = currentSectionName,
                                content = sectionContent,
                                startLine = currentSectionStart + 1,
                                endLine = index
                            )
                        )
                    }
                }

                currentSectionStart = index
                currentSectionName = matchedPattern.nameExtractor(line)
            }
        }

        // Add the last section
        if (currentSectionStart < lines.size) {
            val sectionContent = lines.subList(currentSectionStart, lines.size).joinToString("\n")
            if (sectionContent.isNotBlank()) {
                sections.add(
                    Section(
                        name = currentSectionName,
                        content = sectionContent,
                        startLine = currentSectionStart + 1,
                        endLine = lines.size
                    )
                )
            }
        }

        return sections.ifEmpty {
            // If no sections found, return entire content as one section
            listOf(
                Section(
                    name = null,
                    content = content,
                    startLine = 1,
                    endLine = lines.size
                )
            )
        }
    }

    /**
     * Gets structure patterns based on language.
     */
    private fun getPatternsForLanguage(language: String): List<StructurePattern> {
        return when (language.lowercase()) {
            "kotlin", "kts" -> KOTLIN_PATTERNS
            "java" -> JAVA_PATTERNS
            "markdown", "md" -> MARKDOWN_PATTERNS
            "python", "py" -> PYTHON_PATTERNS
            "javascript", "js", "typescript", "ts" -> JS_TS_PATTERNS
            else -> GENERIC_PATTERNS
        }
    }

    /**
     * Generates a unique chunk ID for a section.
     */
    private fun generateChunkId(source: String, section: Section): String {
        val sourceHash = source.hashCode().toString(16).padStart(8, '0')
        val sectionId = section.name?.sanitizeForId() ?: "${section.startLine}_${section.endLine}"
        return "${sourceHash}_$sectionId"
    }

    private fun String.sanitizeForId(): String {
        return this.replace(Regex("[^a-zA-Z0-9_]"), "_").take(50)
    }

    /**
     * Represents a structural section in the document.
     */
    private data class Section(
        val name: String?,
        val content: String,
        val startLine: Int,
        val endLine: Int
    )

    /**
     * Represents a pattern for detecting structure.
     */
    private data class StructurePattern(
        val regex: Regex,
        val nameExtractor: (String) -> String
    )

    companion object {
        const val MAX_SECTION_SIZE = 2000

        // Kotlin patterns
        private val KOTLIN_PATTERNS = listOf(
            // Class, interface, object declarations
            StructurePattern(
                regex = Regex("^\\s*(public|private|protected|internal|open|sealed|data|value|annotation|enum)?\\s*(class|interface|object)\\s+\\w+"),
                nameExtractor = { line ->
                    Regex("(class|interface|object)\\s+(\\w+)").find(line)?.groupValues?.get(2) ?: "unknown"
                }
            ),
            // Function declarations
            StructurePattern(
                regex = Regex("^\\s*(public|private|protected|internal|suspend|inline|infix|tailrec|operator|override)?\\s*fun\\s+"),
                nameExtractor = { line ->
                    Regex("fun\\s+(\\w+)").find(line)?.groupValues?.get(1) ?: "unknown"
                }
            ),
            // Custom section markers (//================)
            StructurePattern(
                regex = Regex("^\\s*//={3,}.*"),
                nameExtractor = { line ->
                    line.replace(Regex("[/=]"), "").trim().takeIf { it.isNotBlank() } ?: "section"
                }
            )
        )

        // Java patterns
        private val JAVA_PATTERNS = listOf(
            StructurePattern(
                regex = Regex("^\\s*(public|private|protected|abstract|final|static)?\\s*(class|interface|enum)\\s+\\w+"),
                nameExtractor = { line ->
                    Regex("(class|interface|enum)\\s+(\\w+)").find(line)?.groupValues?.get(2) ?: "unknown"
                }
            ),
            StructurePattern(
                regex = Regex("^\\s*(public|private|protected|static|final|synchronized|native)?\\s*\\w+\\s+\\w+\\s*\\("),
                nameExtractor = { line ->
                    Regex("\\s+(\\w+)\\s*\\(").find(line)?.groupValues?.get(1) ?: "unknown"
                }
            )
        )

        // Markdown patterns
        private val MARKDOWN_PATTERNS = listOf(
            StructurePattern(
                regex = Regex("^#{1,6}\\s+.+"),
                nameExtractor = { line ->
                    line.replace(Regex("^#+\\s*"), "").trim()
                }
            ),
            StructurePattern(
                regex = Regex("^```\\w*"),
                nameExtractor = { line ->
                    "code_block_${line.replace("```", "").trim().ifEmpty { "generic" }}"
                }
            ),
            StructurePattern(
                regex = Regex("^---+\\s*$"),
                nameExtractor = { "separator" }
            )
        )

        // Python patterns
        private val PYTHON_PATTERNS = listOf(
            StructurePattern(
                regex = Regex("^\\s*class\\s+\\w+"),
                nameExtractor = { line ->
                    Regex("class\\s+(\\w+)").find(line)?.groupValues?.get(1) ?: "unknown"
                }
            ),
            StructurePattern(
                regex = Regex("^\\s*def\\s+\\w+"),
                nameExtractor = { line ->
                    Regex("def\\s+(\\w+)").find(line)?.groupValues?.get(1) ?: "unknown"
                }
            ),
            StructurePattern(
                regex = Regex("^#\\s*={3,}.*"),
                nameExtractor = { line ->
                    line.replace(Regex("[#=]"), "").trim().takeIf { it.isNotBlank() } ?: "section"
                }
            )
        )

        // JavaScript/TypeScript patterns
        private val JS_TS_PATTERNS = listOf(
            StructurePattern(
                regex = Regex("^\\s*(export\\s+)?(abstract\\s+)?class\\s+\\w+"),
                nameExtractor = { line ->
                    Regex("class\\s+(\\w+)").find(line)?.groupValues?.get(1) ?: "unknown"
                }
            ),
            StructurePattern(
                regex = Regex("^\\s*(export\\s+)?(async\\s+)?function\\s+\\w+"),
                nameExtractor = { line ->
                    Regex("function\\s+(\\w+)").find(line)?.groupValues?.get(1) ?: "unknown"
                }
            ),
            StructurePattern(
                regex = Regex("^\\s*(export\\s+)?(const|let|var)\\s+\\w+\\s*=\\s*(async\\s+)?function|=>"),
                nameExtractor = { line ->
                    Regex("(const|let|var)\\s+(\\w+)").find(line)?.groupValues?.get(2) ?: "unknown"
                }
            )
        )

        // Generic patterns (empty lines as section breaks)
        private val GENERIC_PATTERNS = listOf(
            StructurePattern(
                regex = Regex("^\\s*$"),
                nameExtractor = { "paragraph" }
            )
        )
    }
}
