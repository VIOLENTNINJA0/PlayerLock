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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stars
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.playerlock.data.GameLog
import com.example.playerlock.data.GoatStatusCalculator
import com.example.playerlock.data.GoatStatusResult
import com.example.playerlock.data.PlayerAward
import com.example.playerlock.data.PlayerProfile
import com.example.playerlock.data.PlayoffBracketState
import com.example.playerlock.data.RecordBookCategory
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
    var showGoatHub by remember { mutableStateOf(false) }
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

        val goatResult = remember(gameLogs, awards) {
            GoatStatusCalculator.calculateGoatStatus(gameLogs, awards)
        }

        GoatStatusCard(
            result = goatResult,
            onClick = { showGoatHub = true }
        )
    }

    if (showGoatHub && profile != null) {
        val goatResult = remember(gameLogs, awards) {
            GoatStatusCalculator.calculateGoatStatus(gameLogs, awards)
        }
        GoatHubDialog(
            playerName = profile.playerName,
            gameLogs = gameLogs,
            awards = awards,
            goatResult = goatResult,
            onDismiss = { showGoatHub = false }
        )
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

@Composable
private fun GoatStatusCard(
    result: GoatStatusResult,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tier = result.currentTier

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = "GOAT Status",
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "GOAT STATUS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = primaryColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, primaryColor)
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%,d", result.goatPoints)} GOAT PTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tier ${tier.tierNumber} • ${tier.tierName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val nextText = if (result.nextTier != null) {
                        "Next: ${result.nextTier.tierName} (${String.format(Locale.US, "%,d", result.nextTier.minPoints)} pts)"
                    } else {
                        "#1 Undisputed GOAT Legend"
                    }
                    Text(
                        text = nextText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (result.nextTier != null) {
                LinearProgressIndicator(
                    progress = { result.progressToNextTier },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = primaryColor,
                    trackColor = primaryColor.copy(alpha = 0.2f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to view GOAT Ladder & All-Time Record Books",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "➔",
                    fontSize = 12.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GoatHubDialog(
    playerName: String,
    gameLogs: List<GameLog>,
    awards: List<PlayerAward>,
    goatResult: GoatStatusResult,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = GOAT Ladder, 1 = Record Books
    var selectedCategory by remember { mutableStateOf("Points") }

    val primaryColor = MaterialTheme.colorScheme.primary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
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
                            text = "GOAT STATUS & RECORD BOOKS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Track your rise among NBA All-Time Legends",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Tab Switcher Row (GOAT Ladder | Record Books)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("GOAT Ladder", "Record Books").forEachIndexed { idx, label ->
                        val selected = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) primaryColor else Color.Transparent)
                                .clickable { selectedTab = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Tab Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (selectedTab == 0) {
                        // TAB 1: GOAT Ladder
                        GoatLadderTabContent(goatResult = goatResult)
                    } else {
                        // TAB 2: Record Books
                        RecordBooksTabContent(
                            playerName = playerName,
                            gameLogs = gameLogs,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoatLadderTabContent(goatResult: GoatStatusResult) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val currentTier = goatResult.currentTier
    val breakdown = goatResult.breakdown

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Summary Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    Column {
                        Text(
                            text = "CURRENT GOAT TIER",
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Tier ${currentTier.tierNumber} • ${currentTier.tierName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = primaryColor
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%,d", goatResult.goatPoints)} PTS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                if (goatResult.nextTier != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Next: ${goatResult.nextTier.tierName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${String.format(Locale.US, "%,d", goatResult.nextTier.minPoints)} PTS",
                                style = MaterialTheme.typography.bodySmall,
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { goatResult.progressToNextTier },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = primaryColor,
                            trackColor = primaryColor.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // GOAT Points Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "GOAT POINTS BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                val breakdownRows = listOf(
                    "Championships (+1,500/title)" to breakdown.championshipPts,
                    "Finals MVPs (+1,000/FMVP)" to breakdown.finalsMvpPts,
                    "League MVPs (+1,200/MVP)" to breakdown.mvpPts,
                    "DPOY Awards (+600/DPOY)" to breakdown.dpoyPts,
                    "All-NBA Teams" to breakdown.allNbaPts,
                    "ROTY & All-Star Selections" to (breakdown.royPts + breakdown.allStarPts),
                    "Career Stats Milestones" to breakdown.careerStatsPts,
                    "Triple-Doubles & 50+ PT Games" to (breakdown.tripleDoublePts + breakdown.fiftyPointPts)
                )

                breakdownRows.forEach { (label, valPts) ->
                    if (valPts > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "+${String.format(Locale.US, "%,d", valPts)} pts",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    }
                }
            }
        }

        // 10-Tier Legend Ladder List
        Text(
            text = "10-TIER GOAT LEGEND LADDER",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GoatStatusCalculator.TIERS.sortedBy { it.tierNumber }.forEach { tier ->
                val isCurrent = tier.tierNumber == currentTier.tierNumber
                val isPassed = currentTier.tierNumber < tier.tierNumber

                val containerBg = when {
                    isCurrent -> primaryColor.copy(alpha = 0.12f)
                    isPassed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surface
                }
                val borderColor = when {
                    isCurrent -> primaryColor
                    isPassed -> primaryColor.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = containerBg,
                    border = BorderStroke(if (isCurrent) 2.dp else 1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Rounded.Stars,
                                    contentDescription = "Current Tier",
                                    tint = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (isPassed) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed Tier",
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Locked Tier",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Tier ${tier.tierNumber} • ${tier.tierName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) primaryColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isCurrent) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = primaryColor
                                        ) {
                                            Text(
                                                text = "YOU ARE HERE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "${String.format(Locale.US, "%,d", tier.minPoints)} PTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordBooksTabContent(
    playerName: String,
    gameLogs: List<GameLog>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val categories = listOf("Points", "Rebounds", "Assists", "Steals", "Blocks")

    val userStatValue = remember(selectedCategory, gameLogs) {
        when (selectedCategory) {
            "Points" -> gameLogs.sumOf { it.points }.toLong()
            "Rebounds" -> gameLogs.sumOf { it.rebounds }.toLong()
            "Assists" -> gameLogs.sumOf { it.assists }.toLong()
            "Steals" -> gameLogs.sumOf { it.steals }.toLong()
            "Blocks" -> gameLogs.sumOf { it.blocks }.toLong()
            else -> gameLogs.sumOf { it.points }.toLong()
        }
    }

    val recordCategory = remember(selectedCategory, userStatValue, playerName) {
        GoatStatusCalculator.calculateRecordBookCategory(selectedCategory, playerName, userStatValue)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Category Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSel = selectedCategory == cat
                Surface(
                    onClick = { onCategorySelected(cat) },
                    shape = RoundedCornerShape(50),
                    color = if (isSel) primaryColor else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isSel) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = cat,
                        fontSize = 12.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Category Rank Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    Column {
                        Text(
                            text = "ALL-TIME $selectedCategory RANK",
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${String.format(Locale.US, "%,d", userStatValue)} Total $selectedCategory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = primaryColor
                    ) {
                        Text(
                            text = "#${recordCategory.userRank} ALL-TIME",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                val distanceText = if (recordCategory.userRank == 1) {
                    "🏆 #1 All-Time NBA Leader!"
                } else {
                    "${String.format(Locale.US, "%,d", recordCategory.statsToNextLegend)} $selectedCategory to pass ${recordCategory.nextLegendName} (#${recordCategory.userRank - 1})"
                }

                Text(
                    text = distanceText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )
            }
        }

        // Leaderboard Table
        Text(
            text = "NBA ALL-TIME TOP 10 RANKINGS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                recordCategory.rankings.forEachIndexed { idx, record ->
                    val isUser = record.isUser
                    val rowBg = if (isUser) primaryColor.copy(alpha = 0.15f) else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "#${record.rank}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(32.dp)
                            )

                            Text(
                                text = record.playerName,
                                fontSize = 14.sp,
                                fontWeight = if (isUser) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isUser) primaryColor else MaterialTheme.colorScheme.onSurface
                            )

                            if (isUser) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = primaryColor
                                ) {
                                    Text(
                                        text = "YOU",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = String.format(Locale.US, "%,d", record.statValue),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) primaryColor else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (idx < recordCategory.rankings.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
