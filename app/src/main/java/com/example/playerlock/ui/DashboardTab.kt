package com.example.playerlock.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playerlock.data.GameLog
import com.example.playerlock.data.PlayerAward
import com.example.playerlock.data.PlayerProfile
import com.example.playerlock.data.PlayoffBracketState
import java.util.Locale

private const val AP_XP = 25.0   // 25 XP = 1 AP
private const val BP_XP = 100.0  // 100 XP = 1 BP
private val CardShape = RoundedCornerShape(16.dp)

@Composable
private fun rememberAvatarBitmap(path: String?): ImageBitmap? {
    return remember(path) {
        if (path.isNullOrBlank()) null
        else {
            try {
                BitmapFactory.decodeFile(path)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }
}

@Composable
fun DashboardTab(
    profile: PlayerProfile?,
    gameLogs: List<GameLog>,
    awards: List<PlayerAward> = emptyList(),
    viewModel: PlayerLockViewModel? = null,
    onOvrClick: () -> Unit,
    onSwitchProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }
    val availableUpdate by viewModel?.availableUpdate?.collectAsState() ?: remember { mutableStateOf(null) }
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DASHBOARD",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onSwitchProfile,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Switch Career",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Switch",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Dismissible GitHub Update Banner
        availableUpdate?.let { updateInfo ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = primaryColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, primaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🚀", fontSize = 16.sp)
                        Column {
                            Text(
                                text = "Update Available (${updateInfo.tagName})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "New features & improvements ready for PlayerLock",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.htmlUrl))
                                context.startActivity(intent)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { viewModel?.dismissUpdateBanner() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (profile == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = "No player yet. Create one to get started.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        val seasonGamesCount = gameLogs.count { it.seasonNumber == profile.currentSeason && !it.isPlayoff }

        // ── Playoff Series Banner ─────────────────────────────────────────────
        if (profile.isInPlayoffs) {
            val bracket = remember(profile.playoffBracketJson) {
                PlayoffBracketState.fromJson(profile.playoffBracketJson)
            }
            if (bracket != null) {
                PlayoffSeriesBanner(
                    bracket = bracket,
                    userTeamName = profile.teamName,
                    onViewBracket = { /* no-op: Dashboard has no bracket sheet entry point */ }
                )
            }
        }

        HeroCard(profile = profile, seasonGamesCount = seasonGamesCount, onClick = onOvrClick)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WalletCard(
                modifier = Modifier.weight(1f),
                title = "ATTRIBUTE POINTS",
                amount = profile.availableAp,
                remainder = profile.xpRemainderForAp.toDouble(),
                needed = AP_XP,
                color = MaterialTheme.colorScheme.primary
            )
            WalletCard(
                modifier = Modifier.weight(1f),
                title = "BADGE POINTS",
                amount = profile.availableBp,
                remainder = profile.xpRemainderForBp.toDouble(),
                needed = BP_XP,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        SeasonAveragesCard(logs = gameLogs, currentSeason = profile.currentSeason)

        TrophyCaseCard(awards)
    }

    if (showSettings && viewModel != null) {
        val uiState by viewModel.uiState.collectAsState()
        SettingsDialog(
            settings = uiState.appSettings,
            playerName = profile?.playerName ?: "MyPlayer",
            activeTeamName = profile?.teamName ?: "Draft Prospect",
            viewModel = viewModel,
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun HeroCard(profile: PlayerProfile, seasonGamesCount: Int, onClick: () -> Unit) {
    val avatarBitmap = rememberAvatarBitmap(profile.avatarPath)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // OVR circle or avatar photo
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .border(BorderStroke(3.dp, primaryColor), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap,
                        contentDescription = "${profile.playerName} avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = profile.overallRating.toString(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = "OVR",
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = profile.playerName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = primaryColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, primaryColor)
                    ) {
                        Text(
                            text = if (profile.isInPlayoffs) "Season ${profile.currentSeason} • Playoffs" else "Season ${profile.currentSeason} • Game $seasonGamesCount/82",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = profile.position,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryColor
                    )
                    if (avatarBitmap != null) {
                        Text(
                            text = "${profile.overallRating} OVR",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                }
                Text(
                    text = "${profile.formattedHeight} • ${profile.weightLbs} lbs • #${profile.jerseyNumber} • ${profile.teamName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "Tap to update OVR after editing in 2K",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun WalletCard(
    modifier: Modifier,
    title: String,
    amount: Int,
    remainder: Double,
    needed: Double,
    color: Color
) {
    val progress = (remainder / needed).toFloat().coerceIn(0f, 1f)
    Card(
        modifier = modifier,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Text(
                text = amount.toString(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = "${remainder.toInt()} / ${needed.toInt()} XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun SeasonAveragesCard(logs: List<GameLog>, currentSeason: Int) {
    var mode by remember { mutableIntStateOf(0) } // 0 = Season, 1 = Career

    val displayLogs = remember(logs, mode, currentSeason) {
        if (mode == 0) {
            logs.filter { it.seasonNumber == currentSeason && !it.isPlayoff }
        } else {
            logs.filter { !it.isPlayoff }
        }
    }

    val n = displayLogs.size
    fun avg(total: Int): String =
        if (n == 0) "--" else String.format(Locale.US, "%.1f", total.toDouble() / n)

    val fgm = displayLogs.sumOf { it.fieldGoalsMade }
    val fga = displayLogs.sumOf { it.fieldGoalsAttempted }
    val fgPct = if (fga == 0) "--" else String.format(Locale.US, "%.1f", (fgm * 100.0 / fga).coerceIn(0.0, 100.0))

    val ftm = displayLogs.sumOf { it.freeThrowsMade }
    val fta = displayLogs.sumOf { it.freeThrowsAttempted }
    val ftPct = if (fta == 0) "--" else String.format(Locale.US, "%.1f", (ftm * 100.0 / fta).coerceIn(0.0, 100.0))

    val wins = displayLogs.count { it.isWin }
    val totalMinutes = displayLogs.sumOf { it.minutesPlayed }
    val mpg = avg(totalMinutes)

    val rowOne = listOf(
        "MPG" to mpg,
        "PPG" to avg(displayLogs.sumOf { it.points }),
        "RPG" to avg(displayLogs.sumOf { it.rebounds }),
        "APG" to avg(displayLogs.sumOf { it.assists })
    )
    val rowTwo = listOf(
        "SPG" to avg(displayLogs.sumOf { it.steals }),
        "BPG" to avg(displayLogs.sumOf { it.blocks }),
        "FG%" to fgPct,
        "FT%" to ftPct
    )
    val rowThree = listOf(
        "W-L" to "$wins-${n - wins}",
        "Total MIN" to "$totalMinutes"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (mode == 0) "SEASON AVERAGES" else "CAREER AVERAGES",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Season", "Career").forEachIndexed { idx, label ->
                        val selected = mode == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { mode = idx }
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            listOf(rowOne, rowTwo, rowThree).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { (label, value) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = value,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrophyCaseCard(awards: List<PlayerAward>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                    text = "TROPHY CASE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                if (awards.isNotEmpty()) {
                    Text(
                        text = "${awards.size} TOTAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            if (awards.isEmpty()) {
                Text(
                    text = "No awards or honors claimed yet. Log accolades in the Log Game tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                val awardCounts = remember(awards) {
                    awards.groupBy { it.awardName }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedWith(
                            compareByDescending<Pair<String, Int>> { (name, _) ->
                                when (name) {
                                    "MVP", "Finals MVP" -> 100
                                    "DPOY", "Defensive Player of the Year" -> 90
                                    "Rookie of the Year" -> 85
                                    "Most Improved Player", "Sixth Man of the Year" -> 80
                                    "All-NBA 1st Team" -> 70
                                    "All-NBA 2nd Team" -> 65
                                    "All-NBA 3rd Team" -> 60
                                    "All-Defensive 1st Team" -> 55
                                    "All-Defensive 2nd Team" -> 50
                                    "All-Star" -> 40
                                    "Player of the Week" -> 30
                                    "Player of the Game" -> 20
                                    else -> 10
                                }
                            }.thenByDescending { it.second }
                        )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    awardCounts.forEach { (name, count) ->
                        val (chipBg, chipBorder, chipText) = when {
                            name.contains("MVP", ignoreCase = true) || name.contains("Rookie", ignoreCase = true) || name.contains("DPOY", ignoreCase = true) || name.contains("Defensive Player", ignoreCase = true) -> {
                                Triple(primaryColor.copy(alpha = 0.18f), BorderStroke(1.dp, primaryColor), primaryColor)
                            }
                            name.contains("All-NBA", ignoreCase = true) || name.contains("All-Defensive", ignoreCase = true) || name.contains("All-Star", ignoreCase = true) -> {
                                Triple(secondaryColor.copy(alpha = 0.18f), BorderStroke(1.dp, secondaryColor), secondaryColor)
                            }
                            else -> {
                                Triple(MaterialTheme.colorScheme.surfaceVariant, BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)), MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        val shortLabel = when (name) {
                            "Defensive Player of the Year" -> "DPOY"
                            "Rookie of the Year" -> "ROTY"
                            "Most Improved Player" -> "MIP"
                            "Sixth Man of the Year" -> "6MOY"
                            "Player of the Game" -> "PotG"
                            "Player of the Week" -> "PotW"
                            else -> name
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = chipBg,
                            border = chipBorder
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${count}x",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = chipText
                                )
                                Text(
                                    text = shortLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
