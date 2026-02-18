package ru.agent.features.reasoning.domain.model

/**
 * Predefined reasoning task with a question and expected answer
 */
data class ReasoningTask(
    val id: String,
    val question: String,
    val correctAnswer: String
)

/**
 * Predefined tasks for reasoning comparison
 */
val PREDEFINED_TASKS = listOf(
    ReasoningTask(
        id = "1",
        question = "How many numbers from 1 to 100 are divisible by 3 or 5?",
        correctAnswer = "47"
    ),
    ReasoningTask(
        id = "2",
        question = "All blobs are blabs, all blabs are blubs. Are all blobs blubs?",
        correctAnswer = "Yes"
    ),
    ReasoningTask(
        id = "3",
        question = "Find the error in code: for i in range(10): print(i/0)",
        correctAnswer = "Division by 0"
    ),
    ReasoningTask(
        id = "4",
        question = "17 sheep, all but 9 died. How many are left?",
        correctAnswer = "9"
    )
)
