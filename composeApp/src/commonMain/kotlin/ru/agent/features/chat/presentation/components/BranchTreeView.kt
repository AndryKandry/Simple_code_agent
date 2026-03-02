package ru.agent.features.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.agent.features.chat.domain.model.Branch
import ru.agent.features.chat.domain.model.BranchNode
import ru.agent.features.chat.domain.model.Checkpoint

/**
 * Tree view for displaying checkpoints and branches.
 *
 * @param branchTree The branch tree to display
 * @param checkpoints List of all checkpoints
 * @param branches List of all branches
 * @param currentCheckpointId The currently active checkpoint ID
 * @param onSwitchBranch Callback when a branch is selected
 * @param onCreateBranch Callback to create a new branch from a checkpoint
 * @param onDeleteBranch Callback to delete a branch
 * @param onDeleteCheckpoint Callback to delete a checkpoint
 * @param onSwitchToMain Callback to switch to main branch
 * @param modifier Modifier for the composable
 */
@Composable
fun BranchTreeView(
    branchTree: BranchNode?,
    checkpoints: List<Checkpoint>,
    branches: List<Branch>,
    currentCheckpointId: String?,
    onSwitchBranch: (String?) -> Unit,
    onCreateBranch: (String) -> Unit,
    onDeleteBranch: (String) -> Unit,
    onDeleteCheckpoint: (String) -> Unit,
    onSwitchToMain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Branches",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Branches",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Branch count badge
                Text(
                    text = "${checkpoints.size} checkpoints, ${branches.size} branches",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main branch option
            MainBranchItem(
                isActive = currentCheckpointId == null,
                onClick = onSwitchToMain
            )

            // Branch tree
            if (branchTree != null && branchTree.hasChildren) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(branchTree.children) { node ->
                        BranchTreeNode(
                            node = node,
                            currentCheckpointId = currentCheckpointId,
                            onSwitchBranch = onSwitchBranch,
                            onCreateBranch = onCreateBranch,
                            onDeleteBranch = onDeleteBranch,
                            onDeleteCheckpoint = onDeleteCheckpoint,
                            level = 0
                        )
                    }
                }
            } else if (checkpoints.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No checkpoints yet. Create one to start branching.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Main branch item.
 */
@Composable
private fun MainBranchItem(
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "Main branch",
            tint = if (isActive) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Main Branch",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
        )
        if (isActive) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Active",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Recursive composable for branch tree nodes.
 */
@Composable
private fun BranchTreeNode(
    node: BranchNode,
    currentCheckpointId: String?,
    onSwitchBranch: (String?) -> Unit,
    onCreateBranch: (String) -> Unit,
    onDeleteBranch: (String) -> Unit,
    onDeleteCheckpoint: (String) -> Unit,
    level: Int,
    modifier: Modifier = Modifier
) {
    val isActive = node.checkpoint.id == currentCheckpointId
    val indent = (level * 16).dp

    Column(
        modifier = modifier
    ) {
        // Checkpoint row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = indent)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else Color.Transparent
                )
                .padding(vertical = 6.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Branch icon
            Icon(
                imageVector = if (node.branch != null) Icons.Default.KeyboardArrowRight else Icons.Default.Info,
                contentDescription = "Checkpoint",
                tint = if (isActive) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Checkpoint name
            Text(
                text = node.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Active indicator
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Actions
            Row {
                // Create branch button
                IconButton(
                    onClick = { onCreateBranch(node.checkpoint.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create branch",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Switch button (if not active)
                if (!isActive) {
                    TextButton(
                        onClick = { onSwitchBranch(node.checkpoint.id) },
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Switch",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // Child nodes
        if (node.hasChildren) {
            node.children.forEach { child ->
                BranchTreeNode(
                    node = child,
                    currentCheckpointId = currentCheckpointId,
                    onSwitchBranch = onSwitchBranch,
                    onCreateBranch = onCreateBranch,
                    onDeleteBranch = onDeleteBranch,
                    onDeleteCheckpoint = onDeleteCheckpoint,
                    level = level + 1
                )
            }
        }
    }
}
