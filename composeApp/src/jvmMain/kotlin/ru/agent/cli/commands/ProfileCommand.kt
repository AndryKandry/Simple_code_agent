package ru.agent.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.cli.formatters.OutputFormatter
import ru.agent.features.profile.domain.usecase.GetUserProfileUseCase
import ru.agent.features.profile.domain.usecase.UpdateUserProfileUseCase
import ru.agent.features.profile.domain.usecase.CreateDefaultProfileUseCase

/**
 * Profile command group.
 *
 * Usage:
 * ```
 * agent profile show
 * agent profile update --name "John Doe"
 * ```
 */
class ProfileCommand : CliktCommand(
    name = "profile",
    help = "Manage user profile"
) {
    init {
        subcommands(ProfileShowCommand(), ProfileUpdateCommand())
    }

    override fun run() {
        // Show help if no subcommand
        if (currentContext.invokedSubcommand == null) {
            echo("Please specify a subcommand: show, update")
        }
    }
}

/**
 * Profile show subcommand.
 */
class ProfileShowCommand : CliktCommand(
    name = "show",
    help = "Display current user profile"
) {
    private val terminal = Terminal()

    override fun run() {
        val getUserProfileUseCase: GetUserProfileUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }
        val createDefaultProfileUseCase: CreateDefaultProfileUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        runBlocking {
            var profile = getUserProfileUseCase()

            if (profile == null) {
                profile = createDefaultProfileUseCase()
            }

            terminal.println(OutputFormatter.formatProfile(profile))
        }
    }
}

/**
 * Profile update subcommand.
 */
class ProfileUpdateCommand : CliktCommand(
    name = "update",
    help = "Update user profile"
) {
    private val name by argument(help = "New name")

    private val terminal = Terminal()

    override fun run() {
        val getUserProfileUseCase: GetUserProfileUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }
        val updateUserProfileUseCase: UpdateUserProfileUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        runBlocking {
            val profile = getUserProfileUseCase()

            if (profile != null) {
                val updated = profile.copy(
                    name = name,
                    updatedAt = System.currentTimeMillis()
                )
                updateUserProfileUseCase(updated)
                terminal.println("Profile updated successfully.")
            } else {
                terminal.println("No profile found. Create one first with 'profile show'.")
            }
        }
    }
}
