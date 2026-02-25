package ru.agent.features.chat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.ChatSession
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun ChatSidebar(
    sessions: List<ChatSession>,
    currentSessionId: String?,
    isOpen: Boolean,
    onEvent: (ru.agent.features.chat.presentation.models.ChatEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { -it }),
        exit = slideOutHorizontally(targetOffsetX = { -it }),
        modifier = modifier.clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                ChatSidebarHeader(
                    onCreateNewSession = {
                        onEvent(ru.agent.features.chat.presentation.models.ChatEvent.CreateNewSession)
                    }
                )

                // Sessions list
                if (sessions.isEmpty()) {
                    EmptySessionsState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = sessions,
                            key = { it.id }
                        ) { session ->
                            SessionItem(
                                session = session,
                                isSelected = session.id == currentSessionId,
                                onSelect = {
                                    onEvent(
                                        ru.agent.features.chat.presentation.models.ChatEvent.SelectSession(
                                            session.id
                                        )
                                    )
                                },
                                onDelete = {
                                    onEvent(
                                        ru.agent.features.chat.presentation.models.ChatEvent.DeleteSession(
                                            session.id
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatSidebarHeader(
    onCreateNewSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Chats",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        IconButton(
            onClick = onCreateNewSession
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create new chat",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun SessionItem(
    session: ChatSession,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        Color.White.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.small
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatSessionDate(session.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete session",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EmptySessionsState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No chats yet\nCreate your first chat",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Format session date with relative formatting.
 * Shows "Today HH:MM", "Yesterday", or "DD.MM.YYYY"
 */
@OptIn(ExperimentalTime::class)
private fun formatSessionDate(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val nowInstant = Instant.fromEpochMilliseconds(currentTimeMillis())
    val nowDateTime = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault())

    val hours = localDateTime.hour.toString().padStart(2, '0')
    val minutes = localDateTime.minute.toString().padStart(2, '0')
    val timeString = "$hours:$minutes"

    val sessionDate = localDateTime.date
    val todayDate = nowDateTime.date
    val yesterdayDate = todayDate.minus(1, DateTimeUnit.DAY)

    return when (sessionDate) {
        todayDate -> "Today $timeString"
        yesterdayDate -> "Yesterday"
        else -> {
            val day = localDateTime.day.toString().padStart(2, '0')
            @Suppress("DEPRECATION")
            val month = localDateTime.monthNumber.toString().padStart(2, '0')
            val year = localDateTime.year.toString()
            "$day.$month.$year"
        }
    }
}
