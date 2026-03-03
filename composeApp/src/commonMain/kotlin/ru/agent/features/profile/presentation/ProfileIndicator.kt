package ru.agent.features.profile.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.agent.features.memory.domain.model.ResponseVerbosity
import ru.agent.features.profile.presentation.components.ActivityIndicator
import ru.agent.features.profile.presentation.components.AvatarSize
import ru.agent.features.profile.presentation.components.CompactLanguageBadge
import ru.agent.features.profile.presentation.components.CompactVerbosityIndicator
import ru.agent.features.profile.presentation.components.ProfileAvatar
import ru.agent.features.profile.presentation.components.formatNumber
import ru.agent.features.profile.presentation.components.isRecentlyActive
import ru.agent.features.profile.presentation.theme.ProfileColors

/**
 * Индикатор профиля в TopAppBar.
 *
 * Отображает аватар с initials, имя и роль, а также dropdown меню при клике.
 *
 * @param userName Имя пользователя
 * @param userRole Роль пользователя
 * @param preferredLanguage Предпочтительный язык
 * @param verbosity Уровень детализации ответов
 * @param lastActiveAt Временная метка последней активности
 * @param onClick Обработчик клика для открытия настроек
 * @param onResetDefaults Обработчик сброса настроек
 * @param modifier Modifier
 */
@Composable
fun ProfileIndicator(
    userName: String,
    userRole: String = "developer",
    preferredLanguage: String = "kotlin",
    verbosity: ResponseVerbosity = ResponseVerbosity.NORMAL,
    lastActiveAt: Long? = null,
    onClick: () -> Unit,
    onResetDefaults: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.05f else 1f,
        animationSpec = tween(200),
        label = "avatar_scale"
    )

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { isDropdownExpanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Avatar with activity indicator overlay
            Box {
                ProfileAvatar(
                    name = userName,
                    size = AvatarSize.Indicator,
                    modifier = Modifier.scale(scale)
                )

                // Activity dot overlay
                ActivityIndicator(
                    lastActiveAt = lastActiveAt,
                    size = 8.dp,
                    showPulse = false,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = userRole,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Quick indicators (shown when hovered)
            if (isHovered) {
                Spacer(modifier = Modifier.width(4.dp))

                // Language badge
                CompactLanguageBadge(
                    language = preferredLanguage
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Verbosity indicator
                CompactVerbosityIndicator(
                    verbosity = verbosity
                )
            }
        }

        // Dropdown menu
        ProfileDropdownMenu(
            expanded = isDropdownExpanded,
            onDismiss = { isDropdownExpanded = false },
            userName = userName,
            userRole = userRole,
            preferredLanguage = preferredLanguage,
            verbosity = verbosity,
            lastActiveAt = lastActiveAt,
            onOpenSettings = {
                isDropdownExpanded = false
                onClick()
            },
            onResetDefaults = {
                isDropdownExpanded = false
                onResetDefaults()
            }
        )
    }
}

/**
 * Dropdown меню профиля с расширенной информацией.
 */
@Composable
private fun ProfileDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    userName: String,
    userRole: String,
    preferredLanguage: String,
    verbosity: ResponseVerbosity,
    lastActiveAt: Long?,
    onOpenSettings: () -> Unit,
    onResetDefaults: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(300.dp)
            .padding(8.dp)
    ) {
        // Profile header with avatar and activity
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                ProfileAvatar(
                    name = userName,
                    size = AvatarSize.Dropdown
                )

                // Activity indicator
                ActivityIndicator(
                    lastActiveAt = lastActiveAt,
                    size = 12.dp,
                    showPulse = true,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = userRole,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Activity status
            val isActive = isRecentlyActive(lastActiveAt)
            if (isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Active now",
                    color = ProfileColors.ActiveColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

        // Quick info section with badges
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Language and verbosity row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CompactLanguageBadge(language = preferredLanguage)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Language",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CompactVerbosityIndicator(verbosity = verbosity)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Verbosity",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

        // Actions
        DropdownMenuItem(
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Full Settings")
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Ctrl+P",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            onClick = onOpenSettings
        )

        DropdownMenuItem(
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Reset to Defaults")
                }
            },
            onClick = onResetDefaults
        )
    }
}
