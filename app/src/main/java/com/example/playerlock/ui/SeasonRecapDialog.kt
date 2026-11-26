package com.example.playerlock.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.playerlock.data.SeasonSummary
import java.util.Locale

@Composable
fun SeasonRecapDialog(
    summary: SeasonSummary,
    isCurrentSeasonConclusion: Boolean = false,
    onContinueToPlayoffs: (() -> Unit)? = null,
    onAdvanceToNextSeason: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SEASON ${summary.seasonNumber} RECAP",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                        Text(
                            text = "Final Season Statistics & Upgrades",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVarColor
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = onSurfaceVarColor
                        )
                    }
                }

                if (isCurrentSeasonConclusion) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = primaryColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, primaryColor)
                    ) {
                        Text(
                            text = "🎉 Your season has concluded! You can see your recap anytime in History / Season Recaps.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 1. Season Record & Averages
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceVarColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECORD & STATS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = primaryColor
                            ) {
                                Text(
                                    text = "${summary.wins}-${summary.losses} W-L",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        val rowOne = listOf(
                            "MPG" to String.format(Locale.US, "%.1f", summary.mpg),
                            "PPG" to String.format(Locale.US, "%.1f", summary.ppg),
                            "RPG" to String.format(Locale.US, "%.1f", summary.rpg),
                            "APG" to String.format(Locale.US, "%.1f", summary.apg)
                        )
                        val rowTwo = listOf(
                            "SPG" to String.format(Locale.US, "%.1f", summary.spg),
                            "BPG" to String.format(Locale.US, "%.1f", summary.bpg),
                            "FG%" to String.format(Locale.US, "%.1f%%", summary.fgPct),
                            "FT%" to String.format(Locale.US, "%.1f%%", summary.ftPct)
                        )
                        val rowThree = listOf(
                            "Total MIN" to "${summary.totalMinutes}",
                            "DD / TD" to "${summary.doubleDoubles} / ${summary.tripleDoubles}"
                        )

                        listOf(rowOne, rowTwo, rowThree).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                row.forEach { (label, value) ->
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = value,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = onSurfaceColor
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = onSurfaceVarColor
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Season Best Game",
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurfaceVarColor
                            )
                            Text(
                                text = summary.seasonHighSummary.ifEmpty { "N/A" },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    }
                }

                // 2. OVR & Attribute Upgrades
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceVarColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OVR & ATTRIBUTE PROGRESSION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            val diffOvr = summary.endOvr - summary.startOvr
                            val diffText = if (diffOvr >= 0) "+$diffOvr" else "$diffOvr"
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = secondaryColor
                            ) {
                                Text(
                                    text = "${summary.startOvr} ➔ ${summary.endOvr} ($diffText)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        val attrs = summary.attributeUpgradesSummary.split("; ").filter { it.isNotBlank() && it != "None" }
                        if (attrs.isEmpty()) {
                            Text(
                                text = "No attribute upgrades this season.",
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVarColor
                            )
                        } else {
                            attrs.forEach { item ->
                                Text(
                                    text = "• $item",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = onSurfaceColor
                                )
                            }
                        }
                    }
                }

                // 3. Badge Upgrades
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceVarColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "BADGE PROGRESSION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )

                        val badges = summary.badgeUpgradesSummary.split("; ").filter { it.isNotBlank() && it != "None" }
                        if (badges.isEmpty()) {
                            Text(
                                text = "No badge upgrades this season.",
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVarColor
                            )
                        } else {
                            badges.forEach { item ->
                                Text(
                                    text = "• $item",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = onSurfaceColor
                                )
                            }
                        }
                    }
                }

                // 4. Awards & Total XP
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceVarColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AWARDS & TOTAL XP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Text(
                                text = "+${summary.totalXpEarned} XP Earned",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = secondaryColor
                            )
                        }
                        Text(
                            text = if (summary.awardsSummary.isNotBlank()) summary.awardsSummary else "No awards earned this season.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurfaceColor
                        )
                    }
                }

                // Action Buttons
                if (isCurrentSeasonConclusion) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onContinueToPlayoffs?.invoke()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Continue to Playoffs 🏆", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onAdvanceToNextSeason?.invoke()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, primaryColor)
                        ) {
                            Text("Advance to Season ${summary.seasonNumber + 1} ➔", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryColor)
                        }
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Close Recap", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
