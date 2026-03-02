package ru.agent.features.chat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.agent.features.chat.domain.model.Fact
import ru.agent.features.chat.domain.model.FactCategory
import ru.agent.features.chat.domain.model.FactsByCategory
import ru.agent.features.chat.presentation.theme.ChatColors

/**
 * Side panel for displaying and managing facts in Sticky Facts strategy.
 *
 * Shows facts grouped by category with options to edit/delete.
 */
@Composable
fun FactsPanel(
    factsByCategory: List<FactsByCategory>,
    isLoading: Boolean,
    isExtracting: Boolean,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onDeleteFact: (String) -> Unit,
    onEditFact: (Fact) -> Unit,
    onClearAll: () -> Unit,
    onClearCategory: (FactCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        Box(
            modifier = modifier
                .width(320.dp)
                .fillMaxHeight()
                .background(ChatColors.PanelBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                // Header
                FactsPanelHeader(
                    isLoading = isLoading || isExtracting,
                    onDismiss = onDismiss,
                    onClearAll = onClearAll
                )

                // Loading indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = ChatColors.AccentColor
                        )
                    }
                }

                // Extracting indicator
                if (isExtracting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ChatColors.AccentColor,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Extracting facts...",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChatColors.TextSecondary
                        )
                    }
                }

                // Facts by category
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (factsByCategory.isEmpty() && !isLoading) {
                        item {
                            EmptyFactsContent()
                        }
                    } else {
                        items(factsByCategory) { categoryGroup ->
                            FactCategorySection(
                                factsByCategory = categoryGroup,
                                onDeleteFact = onDeleteFact,
                                onEditFact = onEditFact,
                                onClearCategory = { onClearCategory(categoryGroup.category) }
                            )
                        }
                    }
                }

                // Footer stats
                val totalFacts = factsByCategory.sumOf { it.factCount }
                if (totalFacts > 0) {
                    FactsPanelFooter(
                        totalFacts = totalFacts,
                        categoryCount = factsByCategory.size
                    )
                }
            }
        }
    }
}

@Composable
private fun FactsPanelHeader(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Facts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(
                onClick = onClearAll,
                enabled = !isLoading
            ) {
                Text(
                    text = "Clear All",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLoading) ChatColors.TextSecondary else ChatColors.ErrorColor
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close panel",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun FactCategorySection(
    factsByCategory: FactsByCategory,
    onDeleteFact: (String) -> Unit,
    onEditFact: (Fact) -> Unit,
    onClearCategory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Category header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = factsByCategory.category.icon,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = factsByCategory.category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = "(${factsByCategory.factCount})",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChatColors.TextSecondary
                )
            }

            if (factsByCategory.factCount > 0) {
                TextButton(onClick = onClearCategory) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChatColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Facts in this category
        factsByCategory.facts.forEach { fact ->
            FactItem(
                fact = fact,
                onDelete = { onDeleteFact(fact.id) },
                onEdit = { onEditFact(fact) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun FactItem(
    fact: Fact,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ChatColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = fact.key,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = ChatColors.AccentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = fact.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = ChatColors.TextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // Confidence indicator
                if (fact.confidence < 1.0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Confidence: ${(fact.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChatColors.TextSecondary
                    )
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit fact",
                        tint = ChatColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete fact",
                        tint = ChatColors.ErrorColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFactsContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = ChatColors.TextSecondary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No facts yet",
            style = MaterialTheme.typography.bodyMedium,
            color = ChatColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Facts will be automatically extracted from your conversation",
            style = MaterialTheme.typography.bodySmall,
            color = ChatColors.TextSecondary
        )
    }
}

@Composable
private fun FactsPanelFooter(
    totalFacts: Int,
    categoryCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatColors.PanelFooterBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$totalFacts facts in $categoryCount categories",
                style = MaterialTheme.typography.bodySmall,
                color = ChatColors.TextSecondary
            )
        }
    }
}
