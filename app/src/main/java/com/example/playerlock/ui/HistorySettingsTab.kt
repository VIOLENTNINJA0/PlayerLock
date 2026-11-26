package com.example.playerlock.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playerlock.data.GameLog
import com.example.playerlock.data.PlayerAttribute
import com.example.playerlock.data.PlayerBadge
import com.example.playerlock.data.SeasonSummary
import com.example.playerlock.data.UpgradeHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardShape = RoundedCornerShape(16.dp)
private val EmeraldBadge = Color(0xFF10B981)
private val CoralBadge = Color(0xFFEF4444)

@Composable
fun HistorySettingsTab(
    viewModel: PlayerLockViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var section by remember { mutableIntStateOf(0) }
    var gamePendingDelete by remember { mutableStateOf<GameLog?>(null) }
    var deleteErrorMessage by remember { mutableStateOf<String?>(null) }
    var selectedSummaryForModal by remember { mutableStateOf<SeasonSummary?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Text(
            text = "HISTORY",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp
        )

        // ── Segmented Switcher ────────────────────────────────────────────────
        SegmentedPillSwitcher(
            options = listOf("Game Logs", "Upgrade History", "Season Recaps"),
            selectedIndex = section,
            onOptionSelected = { section = it },
            modifier = Modifier.fillMaxWidth()
        )

        val currentSeason = uiState.profile?.currentSeason ?: 1

        when (section) {
            0 -> GameLogFeed(
                logs = uiState.gameLogs,
                currentSeason = currentSeason,
                onDelete = { gamePendingDelete = it }
            )
            1 -> UpgradeHistoryFeed(
                history = uiState.upgradeHistory,
                attributes = uiState.attributes,
                badges = uiState.badges
            )
            2 -> SeasonRecapsFeed(
                summaries = uiState.seasonSummaries,
                onSelectSummary = { selectedSummaryForModal = it }
            )
        }
    }

    if (selectedSummaryForModal != null) {
        SeasonRecapDialog(
            summary = selectedSummaryForModal!!,
            isCurrentSeasonConclusion = false,
            onDismiss = { selectedSummaryForModal = null }
        )
    }

    // Confirmation Dialog
    gamePendingDelete?.let { game ->
        AlertDialog(
            onDismissRequest = { gamePendingDelete = null },
            title = {
                Text(
                    text = "Reverse Accidental Game Log?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Deleting this game will deduct ${game.xpEarned} XP from your available AP/BP.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val logToDelete = gamePendingDelete
                        gamePendingDelete = null
                        if (logToDelete != null) {
                            viewModel.deleteGameLog(logToDelete) { success, errorMsg ->
                                if (!success && errorMsg != null) {
                                    deleteErrorMessage = errorMsg
                                }
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { gamePendingDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    // Error Feedback Dialog
    deleteErrorMessage?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { deleteErrorMessage = null },
            title = {
                Text(
                    text = "Cannot Delete Game Log",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { deleteErrorMessage = null }) {
                    Text("OK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SegmentedPillSwitcher(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val selected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onOptionSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GameLogFeed(
    logs: List<GameLog>,
    currentSeason: Int,
    onDelete: (GameLog) -> Unit
) {
    if (logs.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = "No games logged yet.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            logs.sortedByDescending { it.id }.forEach { log ->
                GameLogCard(log = log, currentSeason = currentSeason, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun GameLogCard(
    log: GameLog,
    currentSeason: Int,
    onDelete: (GameLog) -> Unit
) {
    val isWin = log.isWin
    val badgeColor = if (isWin) EmeraldBadge else CoralBadge
    val isSeasonActive = log.seasonNumber == currentSeason

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, badgeColor)
                    ) {
                        Text(
                            text = if (isWin) "WIN" else "LOSS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                    Text(
                        text = dateFormat.format(Date(log.datePlayed)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val minPrefix = if (log.minutesPlayed > 0) "${log.minutesPlayed} MIN • " else ""
                Text(
                    text = "$minPrefix${log.points} PTS • ${log.assists} AST • ${log.rebounds} REB • ${log.steals} STL • ${log.blocks} BLK",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Season ${log.seasonNumber} • Teammate Grade: ${log.teammateGrade} • +${log.xpEarned} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isSeasonActive) {
                IconButton(onClick = { onDelete(log) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Game Log",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Season Locked",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Season Locked",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun UpgradeHistoryFeed(
    history: List<UpgradeHistory>,
    attributes: List<PlayerAttribute>,
    badges: List<PlayerBadge>
) {
    if (history.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = "No upgrade history yet. Upgrade attributes or badges in the Shop to see them here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        val tiers = listOf("None", "Bronze", "Silver", "Gold", "HOF", "Legend")
        val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            history.forEach { item ->
                val (title, detailText) = when (item.entityType) {
                    "ATTRIBUTE" -> {
                        val attrName = attributes.find { it.id == item.entityId }?.attributeName ?: "Attribute"
                        attrName to "${item.oldValue} ➔ ${item.newValue}"
                    }
                    "BADGE" -> {
                        val badgeName = badges.find { it.id == item.entityId }?.badgeName ?: "Badge"
                        val oldTier = tiers.getOrElse(item.oldValue) { "None" }
                        val newTier = tiers.getOrElse(item.newValue) { "None" }
                        badgeName to "$oldTier ➔ $newTier"
                    }
                    else -> "Upgrade" to "${item.oldValue} ➔ ${item.newValue}"
                }

                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val typeColor = if (item.entityType == "ATTRIBUTE") primaryColor else secondaryColor

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = typeColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, typeColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = item.entityType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = typeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "Season ${item.seasonNumber}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = detailText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryColor
                                )
                                Text(
                                    text = "• ${dateFormat.format(Date(item.timestamp))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "-${item.costSpent} ${item.currencyType}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (item.currencyType == "AP") primaryColor else secondaryColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (item.isAppliedIn2k) EmeraldBadge.copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, if (item.isAppliedIn2k) EmeraldBadge else primaryColor)
                            ) {
                                Text(
                                    text = if (item.isAppliedIn2k) "Applied 2K" else "Pending 2K",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isAppliedIn2k) EmeraldBadge else primaryColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonRecapsFeed(
    summaries: List<SeasonSummary>,
    onSelectSummary: (SeasonSummary) -> Unit
) {
    if (summaries.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = "No season recaps archived yet. Complete an 82-game season to archive your first recap.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        val sortedSummaries = remember(summaries) { summaries.sortedByDescending { it.seasonNumber } }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            sortedSummaries.forEach { summary ->
                Card(
                    onClick = { onSelectSummary(summary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(
                                        text = "${summary.wins}-${summary.losses}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Season ${summary.seasonNumber} Recap",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${String.format(Locale.US, "%.1f", summary.ppg)} PPG • ${String.format(Locale.US, "%.1f", summary.rpg)} REB • ${String.format(Locale.US, "%.1f", summary.apg)} AST",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "OVR: ${summary.startOvr} ➔ ${summary.endOvr} • +${summary.totalXpEarned} XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "View ➔",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
