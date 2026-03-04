package ru.agent.features.task.domain.usecase

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.repository.ChatRepository
import ru.agent.features.task.domain.model.PlanStep
import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.repository.TaskStateRepository

/**
 * Use case for generating a task plan using LLM.
 * Analyzes the user's request and creates a structured plan.
 */
class GenerateTaskPlanUseCase(
    private val chatRepository: ChatRepository,
    private val taskStateRepository: TaskStateRepository
) {
    /**
     * Generates a plan for the given task.
     * Uses silent message (not saved to chat history).
     *
     * @param sessionId The chat session ID
     * @param taskState The task to plan
     * @param userRequest The original user request
     * @return Updated task state with the plan
     */
    suspend operator fun invoke(
        sessionId: String,
        taskState: TaskState,
        userRequest: String
    ): TaskState {
        // Create planning prompt
        val planningPrompt = createPlanningPrompt(userRequest)

        // Send to LLM (silent - not saved to chat history)
        val planResponse = when (val result = chatRepository.sendSilentMessage(sessionId, planningPrompt)) {
            is ResultWrapper.Success -> result.value
            is ResultWrapper.Error -> return taskState.copy(
                plan = "Failed to generate plan: ${result.message}",
                expectedAction = "Error generating plan. Click 'Start' to proceed anyway."
            )
        }

        // Parse the response
        val (planText, planSteps) = parsePlanResponse(planResponse)

        // Update task with plan
        val updatedTask = taskState.copy(
            plan = planText,
            planSteps = planSteps,
            totalSteps = planSteps.size.coerceAtLeast(1),
            expectedAction = "Plan ready. Click 'Start Execution' to begin."
        )

        // Save updated task
        taskStateRepository.saveTaskState(updatedTask)

        return updatedTask
    }

    /**
     * Creates a prompt for the LLM to generate a plan.
     */
    private fun createPlanningPrompt(userRequest: String): String {
        return """
            You are a task planning assistant. Analyze the following request and create a clear, step-by-step plan.

            USER REQUEST:
            $userRequest

            INSTRUCTIONS:
            1. Break down the request into logical steps
            2. Each step should be specific and actionable
            3. Number each step
            4. Keep it concise (max 5-7 steps)

            FORMAT YOUR RESPONSE EXACTLY LIKE THIS:

            **Summary:** [One sentence summary of the task]

            **Plan:**
            1. [First step]
            2. [Second step]
            3. [Third step]
            ...

            **Estimated complexity:** [Simple/Medium/Complex]

            RESPOND ONLY WITH THE PLAN, NO OTHER TEXT.
        """.trimIndent()
    }

    /**
     * Parses the LLM response into a structured plan.
     */
    private fun parsePlanResponse(response: String): Pair<String, List<PlanStep>> {
        val steps = mutableListOf<PlanStep>()
        val lines = response.lines()

        var stepNumber = 0
        for (line in lines) {
            val trimmedLine = line.trim()
            // Match lines like "1. Step description" or "1) Step description"
            val stepMatch = STEP_PATTERN.find(trimmedLine)
            if (stepMatch != null) {
                stepNumber++
                val description = stepMatch.groupValues[2].trim()
                if (description.isNotEmpty()) {
                    steps.add(PlanStep(
                        number = stepNumber,
                        description = description
                    ))
                }
            }
        }

        // If no steps found, create a default single step
        if (steps.isEmpty()) {
            steps.add(PlanStep(
                number = 1,
                description = "Execute the task"
            ))
        }

        return Pair(response, steps)
    }

    companion object {
        private val STEP_PATTERN = Regex("^(\\d+)[.)]\\s*(.+)$")
    }
}
