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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.memory.domain.model.UserProfile
import ru.agent.features.profile.presentation.components.ActivityIndicator
import ru.agent.features.profile.presentation.components.AvatarSize
import ru.agent.features.profile.presentation.components.CompactLanguageBadge
import ru.agent.features.profile.presentation.components.ProfileAvatar
import ru.agent.features.profile.presentation.components.ProfileStatsRow
import ru.agent.features.profile.presentation.components.formatNumber
import ru.agent.features.profile.presentation.components.isRecentlyActive
import ru.agent.features.profile.presentation.theme.ProfileColors

/**
 * Dashboard карточка профиля пользователя.
 *
 * Отображает полную информацию о профиле с avatar, ролью,
 * бейджами языков и статистикой.
 *
 * @param profile Профиль пользователя
 * @param isCompact Компактный режим для sidebar
 * @param onEditClick Обработчик клика на редактирование
 * @param onSettingsClick Обработчик клика на настройки
 * @param modifier Modifier
 */
@Composable
fun ProfileDashboard(
    profile: UserProfile,
    isCompact: Boolean = false,
    onEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(200),
        label = "dashboard_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            ProfileColors.CardGradientStart,
                            ProfileColors.CardGradientEnd
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(if (isCompact) 16.dp else 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with activity indicator
                Box {
                    ProfileAvatar(
                        name = profile.name,
                        size = if (isCompact) AvatarSize.Dropdown else AvatarSize.Dialog
                    )

                    // Activity dot on avatar
                    ActivityIndicator(
                        lastActiveAt = profile.interactionStats.lastActiveAt,
                        size = 10.dp,
                        showPulse = true,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

                // Name
                Text(
                    text = profile.name,
                    color = Color.White,
                    fontSize = if (isCompact) 16.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Role
                Text(
                    text = profile.role,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = if (isCompact) 12.sp else 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!isCompact) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Language badge
                    CompactLanguageBadge(
                        language = profile.preferences.preferredLanguage
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats
                    ProfileStatsRow(
                        totalMessages = profile.interactionStats.totalMessages,
                        totalSessions = profile.interactionStats.totalSessions,
                        totalTasksCompleted = profile.interactionStats.totalTasksCompleted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Last active
                    profile.interactionStats.lastActiveAt?.let { lastActive ->
                        Text(
                            text = formatLastActive(lastActive),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = onEditClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit",
                                color = Color.White
                            )
                        }

                        TextButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Settings",
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Compact mode: just language indicator
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactLanguageBadge(
                            language = profile.preferences.preferredLanguage
                        )

                        val isActive = isRecentlyActive(profile.interactionStats.lastActiveAt)
                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Active",
                                color = ProfileColors.ActiveColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Форматирование времени последней активности.
 *
 * @param timestamp Временная метка (ms)
 * @return Отформатированная строка
 */
private fun formatLastActive(timestamp: Long): String {
    val now = currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Active now"
        diff < 3600_000 -> "Active ${diff / 60_000}m ago"
        diff < 86400_000 -> "Active ${diff / 3600_000}h ago"
        diff < 604800_000 -> "Active ${diff / 86400_000}d ago"
        else -> "Active long ago"
    }
}
