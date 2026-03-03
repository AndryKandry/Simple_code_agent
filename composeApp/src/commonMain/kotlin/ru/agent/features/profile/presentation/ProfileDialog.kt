package ru.agent.features.profile.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.koin.compose.viewmodel.koinViewModel
import ru.agent.features.memory.domain.model.ResponseVerbosity
import ru.agent.features.memory.domain.model.UserProfile
import ru.agent.features.profile.presentation.components.AvatarSize
import ru.agent.features.profile.presentation.components.ProfileAvatar
import ru.agent.features.profile.presentation.components.SettingOptionCard
import ru.agent.features.profile.presentation.theme.ProfileColors
import ru.agent.features.profile.presentation.theme.getLanguageColor

/**
 * Диалог настроек профиля пользователя с улучшенным UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.viewStates().collectAsState()
    val action by viewModel.viewActions().collectAsState(null)
    val focusRequester = remember { FocusRequester() }

    // Handle save action
    LaunchedEffect(action) {
        when (action) {
            is ProfileAction.ProfileSaved -> {
                onSaved()
                viewModel.clearAction()
            }
            is ProfileAction.DismissDialog -> {
                onDismiss()
                viewModel.clearAction()
            }
            else -> {}
        }
    }

    // Request focus when dialog opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    when {
                        // Ctrl+S to save
                        keyEvent.isCtrlPressed && keyEvent.key == Key.S -> {
                            if (!state.isSaving) {
                                viewModel.obtainEvent(ProfileEvent.SaveProfile)
                            }
                            true
                        }
                        // Escape to close
                        keyEvent.key == Key.Escape -> {
                            onDismiss()
                            true
                        }
                        else -> false
                    }
                },
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile Settings",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Form content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        // Profile Card with avatar
                        ProfileCard(
                            profile = state.profile,
                            selectedColorIndex = state.avatarColorIndex,
                            onColorSelected = { index ->
                                viewModel.obtainEvent(ProfileEvent.AvatarColorChanged(index))
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Basic Info Section
                        SettingsSection(
                            icon = Icons.Outlined.Person,
                            title = "Basic Info",
                            initiallyExpanded = true
                        ) {
                            OutlinedTextField(
                                value = state.name,
                                onValueChange = { viewModel.obtainEvent(ProfileEvent.NameChanged(it)) },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.role,
                                onValueChange = { viewModel.obtainEvent(ProfileEvent.RoleChanged(it)) },
                                label = { Text("Role") },
                                placeholder = { Text("developer, manager, designer...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = state.context,
                                onValueChange = { viewModel.obtainEvent(ProfileEvent.ContextChanged(it)) },
                                label = { Text("Context") },
                                placeholder = { Text("Additional context about you...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Language Selector with visual cards
                            Text(
                                text = "Preferred Language",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LanguageSelector(
                                selectedLanguage = state.preferredLanguage,
                                onLanguageSelected = { viewModel.obtainEvent(ProfileEvent.LanguageChanged(it)) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Response Verbosity with segmented buttons
                            Text(
                                text = "Response Verbosity",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            VerbositySelector(
                                selectedVerbosity = state.responseVerbosity,
                                onVerbositySelected = { viewModel.obtainEvent(ProfileEvent.VerbosityChanged(it)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Code Style Section
                        SettingsSection(
                            icon = Icons.Default.Settings,
                            title = "Code Style"
                        ) {
                            OutlinedTextField(
                                value = state.indentSize.toString(),
                                onValueChange = {
                                    it.toIntOrNull()?.let { size ->
                                        viewModel.obtainEvent(ProfileEvent.IndentSizeChanged(size))
                                    }
                                },
                                label = { Text("Indent Size") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = state.maxLineLength.toString(),
                                onValueChange = {
                                    it.toIntOrNull()?.let { length ->
                                        viewModel.obtainEvent(ProfileEvent.MaxLineLengthChanged(length))
                                    }
                                },
                                label = { Text("Max Line Length") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = state.useTabs,
                                    onCheckedChange = {
                                        viewModel.obtainEvent(ProfileEvent.UseTabsChanged(it))
                                    }
                                )
                                Text("Use Tabs")
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = state.trailingComma,
                                    onCheckedChange = {
                                        viewModel.obtainEvent(ProfileEvent.TrailingCommaChanged(it))
                                    }
                                )
                                Text("Trailing Comma")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Communication Section
                        SettingsSection(
                            icon = Icons.Default.Email,
                            title = "Communication"
                        ) {
                            OutlinedTextField(
                                value = state.customInstructions.joinToString("\n"),
                                onValueChange = { text ->
                                    val instructions = text.split("\n").filter { it.isNotBlank() }
                                    viewModel.obtainEvent(ProfileEvent.CustomInstructionsChanged(instructions))
                                },
                                label = { Text("Custom Instructions") },
                                placeholder = { Text("One instruction per line...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 6
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // System Prompt Preview
                        SettingsSection(
                            icon = Icons.Outlined.Build,
                            title = "System Prompt Preview"
                        ) {
                            val clipboardManager: ClipboardManager = LocalClipboardManager.current
                            val systemPrompt = generateSystemPromptPreview(state)
                            var copied by remember { mutableStateOf(false) }

                            // Reset copied state after 2 seconds
                            LaunchedEffect(copied) {
                                if (copied) {
                                    kotlinx.coroutines.delay(2000)
                                    copied = false
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Generated System Prompt",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(systemPrompt))
                                                copied = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (copied) Icons.Default.Check else Icons.Default.Done,
                                                contentDescription = if (copied) "Copied" else "Copy",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = systemPrompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 8,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Error message
                        state.error?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel (Esc)")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.obtainEvent(ProfileEvent.SaveProfile) },
                        enabled = !state.isSaving && !state.isLoading
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Save (Ctrl+S)")
                    }
                }
            }
        }
    }
}

/**
 * Profile Card с аватаром и выбором цвета.
 */
@Composable
private fun ProfileCard(
    profile: UserProfile?,
    selectedColorIndex: Int,
    onColorSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileAvatar(
                name = profile?.name ?: "User",
                size = AvatarSize.Dialog
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Choose Avatar Color",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileColors.AvatarColors.forEachIndexed { index, color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onColorSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (index == selectedColorIndex) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Expandable секция настроек с иконкой и заголовком.
 */
@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(200),
        label = "expand_rotation"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotation)
                )
            }

            // Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Селектор языка программирования с визуальными карточками.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageSelector(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val languages = listOf(
        "kotlin" to "Kotlin",
        "python" to "Python",
        "typescript" to "TypeScript",
        "javascript" to "JavaScript",
        "rust" to "Rust",
        "go" to "Go",
        "java" to "Java",
        "swift" to "Swift"
    )

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        languages.forEach { (id, name) ->
            SettingOptionCard(
                title = name,
                accentColor = getLanguageColor(id),
                isSelected = selectedLanguage.equals(id, ignoreCase = true),
                onClick = { onLanguageSelected(id) },
                modifier = Modifier.fillMaxWidth(0.3f)
            )
        }
    }
}

/**
 * Селектор уровня детализации с segmented buttons.
 */
@Composable
private fun VerbositySelector(
    selectedVerbosity: ResponseVerbosity,
    onVerbositySelected: (ResponseVerbosity) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        ResponseVerbosity.CONCISE to "Brief",
        ResponseVerbosity.NORMAL to "Normal",
        ResponseVerbosity.DETAILED to "Detailed"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (verbosity, label) ->
            VerbosityOption(
                label = label,
                isSelected = selectedVerbosity == verbosity,
                onClick = { onVerbositySelected(verbosity) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Опция для селектора детализации.
 */
@Composable
private fun VerbosityOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        ProfileColors.SelectedOptionBackground
    } else {
        ProfileColors.SectionBackground
    }

    val borderColor = if (isSelected) {
        ProfileColors.SelectedOptionBorder
    } else {
        ProfileColors.SectionBorder
    }

    val textColor = if (isSelected) {
        androidx.compose.ui.graphics.Color.White
    } else {
        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

/**
 * Генерация preview system prompt.
 */
private fun generateSystemPromptPreview(state: ProfileState): String {
    val parts = mutableListOf<String>()

    parts.add("Name: ${state.name}")
    parts.add("Role: ${state.role}")

    if (state.context.isNotBlank()) {
        parts.add("Context: ${state.context}")
    }

    parts.add("Language: ${state.preferredLanguage}")
    parts.add("Verbosity: ${state.responseVerbosity}")

    parts.add("Code Style:")
    parts.add("  - Indent: ${state.indentSize} spaces")
    parts.add("  - Max line length: ${state.maxLineLength}")
    parts.add("  - Use tabs: ${state.useTabs}")
    parts.add("  - Trailing comma: ${state.trailingComma}")

    if (state.customInstructions.isNotEmpty()) {
        parts.add("Custom Instructions:")
        state.customInstructions.forEach { instruction ->
            parts.add("  - $instruction")
        }
    }

    return parts.joinToString("\n")
}
