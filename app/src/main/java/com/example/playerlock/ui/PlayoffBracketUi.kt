package com.example.playerlock.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.playerlock.data.NbaTeamCatalog
import com.example.playerlock.data.NbaTeamScheme
import com.example.playerlock.data.PlayoffBracketState
import com.example.playerlock.data.PlayoffSeries
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// 1. Bracket Setup Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlayoffBracketSetupDialog(
    userTeamName: String,
    userTeamConference: String,
    onTipOff: (PlayoffBracketState) -> Unit,
    onDismiss: () -> Unit
) {
    val conferenceTeams = remember(userTeamConference) {
        NbaTeamCatalog.getTeamsForConference(userTeamConference)
    }
    val userScheme = remember(userTeamName) { NbaTeamCatalog.getScheme(userTeamName) }
    val opposingConf = if (userTeamConference == "West") "East" else "West"
    val opposingTeams = remember(opposingConf) { NbaTeamCatalog.getTeamsForConference(opposingConf) }

    // Seed selection (1–8)
    var userSeed by remember { mutableIntStateOf(1) }
    // 8 slots: index 0 = seed 1, index 7 = seed 8. Each slot holds an abbr or "" (empty)
    val slotAbbrs = remember { mutableStateListOf<String>().also { list -> repeat(8) { list.add("") } } }
    var showTeamPickerForSlot by remember { mutableStateOf<Int?>(null) }
    var opposingChampAbbr by remember { mutableStateOf(opposingTeams.firstOrNull()?.abbreviation ?: "BOS") }
    var showOpposingPicker by remember { mutableStateOf(false) }

    // Always lock user's team into the user-seed slot
    LaunchedEffect(userSeed) {
        slotAbbrs[userSeed - 1] = userScheme.abbreviation
    }

    // Fill remaining seeds with defaults from the conference (excluding user's team)
    fun fillRemaining() {
        val remaining = conferenceTeams.filter { it.abbreviation != userScheme.abbreviation }.toMutableList()
        for (i in 0 until 8) {
            if (i == userSeed - 1) continue // skip user's slot
            if (slotAbbrs[i].isEmpty() || slotAbbrs[i] == userScheme.abbreviation) {
                val pick = remaining.removeFirstOrNull() ?: continue
                slotAbbrs[i] = pick.abbreviation
            }
        }
    }

    // Build bracket on tip-off
    fun buildBracket(): PlayoffBracketState {
        // Standard NBA bracket: 1v8, 4v5, 3v6, 2v7
        val seeds = slotAbbrs.toList().mapIndexed { idx, abbr -> (idx + 1) to abbr }
        fun abbr(seed: Int) = seeds.firstOrNull { it.first == seed }?.second ?: ""
        val matchups = listOf(
            Triple(1, 8, "r1_1v8"),
            Triple(4, 5, "r1_4v5"),
            Triple(3, 6, "r1_3v6"),
            Triple(2, 7, "r1_2v7")
        ).map { (top, bot, id) ->
            PlayoffSeries(
                id = id, round = 1,
                topTeamAbbr = abbr(top), topSeed = top,
                bottomTeamAbbr = abbr(bot), bottomSeed = bot,
                isUserSeries = (top == userSeed || bot == userSeed)
            )
        }
        return PlayoffBracketState(
            userConference = userTeamConference,
            userSeed = userSeed,
            userTeamAbbr = userScheme.abbreviation,
            currentRound = 1,
            finalsOpponentAbbr = opposingChampAbbr,
            round1Matchups = matchups
        )
    }

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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🏆 PLAYOFF BRACKET SETUP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$userTeamConference Conference • ${userScheme.abbreviation}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Seed Selector
                Text(
                    text = "YOUR SEED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..8).forEach { seed ->
                        val selected = userSeed == seed
                        Surface(
                            onClick = {
                                // Before switching seed, clear the old slot if it held the user's abbr
                                val oldSlot = userSeed - 1
                                if (slotAbbrs[oldSlot] == userScheme.abbreviation) slotAbbrs[oldSlot] = ""
                                userSeed = seed
                                slotAbbrs[seed - 1] = userScheme.abbreviation
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "#$seed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Conference Bracket Slots
                Text(
                    text = "${userTeamConference.uppercase()} CONFERENCE SEEDS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..8).forEach { seed ->
                        val isUserSlot = seed == userSeed
                        val abbr = slotAbbrs[seed - 1]
                        val scheme = if (abbr.isNotEmpty()) NbaTeamCatalog.getSchemeByAbbr(abbr) else null
                        SeedSlotRow(
                            seed = seed,
                            abbr = abbr,
                            scheme = scheme,
                            isUserSlot = isUserSlot,
                            onClick = if (isUserSlot) null else { { showTeamPickerForSlot = seed } }
                        )
                    }
                }

                // Fill Remaining Button
                OutlinedButton(
                    onClick = { fillRemaining() },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Text(
                        text = "⚡ Fill Remaining Seeds",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Opposing Conference Finals Favorite
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Text(
                    text = "NBA FINALS OPPONENT ($opposingConf Conference)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                val oppScheme = NbaTeamCatalog.getSchemeByAbbr(opposingChampAbbr)
                Surface(
                    onClick = { showOpposingPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, oppScheme.primaryColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TeamColorPill(scheme = oppScheme, size = 32.dp)
                            Column {
                                Text(
                                    text = oppScheme.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(text = "Tap to change", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(text = "Change ▼", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Tip Off Button
                Button(
                    onClick = { onTipOff(buildBracket()) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("🏀 Tip Off Playoffs", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Team picker for a conference seed slot
    if (showTeamPickerForSlot != null) {
        val slot = showTeamPickerForSlot!!
        val usedAbbrs = slotAbbrs.filterIndexed { idx, _ -> idx != slot - 1 }.filter { it.isNotEmpty() }.toSet()
        val available = conferenceTeams.filter { it.abbreviation !in usedAbbrs }
        TeamPickerDialog(
            title = "Pick Team for Seed #$slot",
            teams = available,
            onSelect = { abbr ->
                slotAbbrs[slot - 1] = abbr
                showTeamPickerForSlot = null
            },
            onDismiss = { showTeamPickerForSlot = null }
        )
    }

    // Opposing conference champion picker
    if (showOpposingPicker) {
        TeamPickerDialog(
            title = "Pick $opposingConf Conference Favorite",
            teams = opposingTeams,
            onSelect = { abbr ->
                opposingChampAbbr = abbr
                showOpposingPicker = false
            },
            onDismiss = { showOpposingPicker = false }
        )
    }
}

@Composable
private fun SeedSlotRow(
    seed: Int,
    abbr: String,
    scheme: NbaTeamScheme?,
    isUserSlot: Boolean,
    onClick: (() -> Unit)?
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(12.dp),
        color = when {
            isUserSlot -> primary.copy(alpha = 0.12f)
            abbr.isNotEmpty() -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = when {
            isUserSlot -> BorderStroke(1.5.dp, primary)
            abbr.isNotEmpty() && scheme != null -> BorderStroke(1.dp, scheme.primaryColor.copy(alpha = 0.4f))
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "#$seed",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = if (isUserSlot) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )
            if (scheme != null) {
                TeamColorPill(scheme = scheme, size = 28.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = scheme.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (isUserSlot) Text(text = "YOU", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = primary)
                }
            } else {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) { Text("?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(
                    text = "Tap to pick team",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
            }
            if (!isUserSlot && abbr.isNotEmpty()) {
                Text("✎", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun TeamPickerDialog(
    title: String,
    teams: List<NbaTeamScheme>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                teams.forEach { team ->
                    Surface(
                        onClick = { onSelect(team.abbreviation) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TeamColorPill(scheme = team, size = 28.dp)
                            Text(text = team.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Live Playoff Series Banner (shown at top of LogGameTab & DashboardTab)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlayoffSeriesBanner(
    bracket: PlayoffBracketState,
    userTeamName: String,
    onViewBracket: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userSeries = bracket.activeUserSeries
    val userScheme = remember(userTeamName) { NbaTeamCatalog.getScheme(userTeamName) }

    val goldColor = Color(0xFFFFD700)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, goldColor.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            // Round label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 ${bracket.currentRoundName.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = goldColor,
                    letterSpacing = 1.sp
                )
                TextButton(
                    onClick = onViewBracket,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "View Bracket →",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (userSeries != null) {
                Spacer(Modifier.height(6.dp))
                SeriesScoreRow(series = userSeries, userSeed = bracket.userSeed)
            }
        }
    }
}

@Composable
private fun SeriesScoreRow(series: PlayoffSeries, userSeed: Int) {
    val topScheme  = NbaTeamCatalog.getSchemeByAbbr(series.topTeamAbbr)
    val botScheme  = NbaTeamCatalog.getSchemeByAbbr(series.bottomTeamAbbr)
    val userIsTop  = series.topSeed == userSeed
    val userWins   = if (userIsTop) series.topWins else series.bottomWins
    val oppWins    = if (userIsTop) series.bottomWins else series.topWins
    val userAbbr   = if (userIsTop) series.topTeamAbbr else series.bottomTeamAbbr
    val oppAbbr    = if (userIsTop) series.bottomTeamAbbr else series.topTeamAbbr
    val userScheme = if (userIsTop) topScheme else botScheme
    val oppScheme  = if (userIsTop) botScheme else topScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // User team
        TeamColorPill(scheme = userScheme, size = 36.dp)
        Spacer(Modifier.width(6.dp))
        Text(text = userAbbr, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = userScheme.primaryColor)
        Spacer(Modifier.width(10.dp))
        // Win pips
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { idx ->
                WinPip(filled = idx < userWins, color = userScheme.primaryColor)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "–",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { idx ->
                WinPip(filled = idx < oppWins, color = oppScheme.primaryColor)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(text = oppAbbr, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = oppScheme.primaryColor)
        Spacer(Modifier.width(6.dp))
        TeamColorPill(scheme = oppScheme, size = 36.dp)
    }
}

@Composable
private fun WinPip(filled: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(if (filled) color else color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.5f), CircleShape)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Interactive Playoff Bracket Sheet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlayoffBracketSheet(
    bracket: PlayoffBracketState,
    userSeed: Int,
    onSetCpuWinner: (seriesId: String, winnerAbbr: String) -> Unit,
    onCompleteRemainingCpuSeries: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🏆 PLAYOFF BRACKET",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${bracket.userConference} Conference + NBA Finals",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // "Complete Remaining CPU Series" button — only when there are unfinished CPU series
                val unfinishedCpuSeries = when (bracket.currentRound) {
                    1 -> bracket.round1Matchups.filter { !it.isUserSeries && !it.isFinished }
                    2 -> bracket.semisMatchups.filter { !it.isUserSeries && !it.isFinished }
                    3 -> listOfNotNull(bracket.confFinalsMatchup).filter { !it.isFinished }
                    4 -> listOfNotNull(bracket.nbaFinalsMatchup).filter { !it.isFinished }
                    else -> emptyList()
                }
                if (unfinishedCpuSeries.isNotEmpty()) {
                    Button(
                        onClick = onCompleteRemainingCpuSeries,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text(
                            text = "⚡ Complete Remaining CPU Series",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Row horizontally scrollable bracket
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Round 1 — 4 series
                    BracketRoundColumn(
                        title = "ROUND 1",
                        series = bracket.round1Matchups,
                        userSeed = userSeed,
                        onSetCpuWinner = onSetCpuWinner
                    )

                    BracketConnectorColumn(count = 4)

                    // Conf Semis — 2 series
                    BracketRoundColumn(
                        title = "CONF. SEMIS",
                        series = bracket.semisMatchups,
                        userSeed = userSeed,
                        onSetCpuWinner = onSetCpuWinner,
                        topPaddingDp = 44.dp
                    )

                    BracketConnectorColumn(count = 2)

                    // Conf Finals — 1 series
                    BracketRoundColumn(
                        title = "CONF. FINALS",
                        series = listOfNotNull(bracket.confFinalsMatchup),
                        userSeed = userSeed,
                        onSetCpuWinner = onSetCpuWinner,
                        topPaddingDp = 100.dp
                    )

                    BracketConnectorColumn(count = 1)

                    // NBA Finals — 1 series
                    BracketRoundColumn(
                        title = "NBA FINALS",
                        series = listOfNotNull(bracket.nbaFinalsMatchup),
                        userSeed = userSeed,
                        onSetCpuWinner = onSetCpuWinner,
                        topPaddingDp = 100.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun BracketRoundColumn(
    title: String,
    series: List<PlayoffSeries>,
    userSeed: Int,
    onSetCpuWinner: (String, String) -> Unit,
    topPaddingDp: Dp = 0.dp
) {
    Column(
        modifier = Modifier.width(160.dp).padding(top = topPaddingDp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (series.isEmpty()) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("TBD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        } else {
            series.forEach { s ->
                BracketSeriesCard(
                    series = s,
                    userSeed = userSeed,
                    onSetCpuWinner = onSetCpuWinner
                )
            }
        }
    }
}

@Composable
private fun BracketSeriesCard(
    series: PlayoffSeries,
    userSeed: Int,
    onSetCpuWinner: (String, String) -> Unit
) {
    val topScheme = NbaTeamCatalog.getSchemeByAbbr(series.topTeamAbbr)
    val botScheme = NbaTeamCatalog.getSchemeByAbbr(series.bottomTeamAbbr)
    val isUserSeries = series.isUserSeries
    val goldColor = Color(0xFFFFD700)
    val winner = series.winnerAbbr
    var showWinnerPicker by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (isUserSeries) 1.5.dp else 1.dp,
            color = if (isUserSeries) goldColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isUserSeries) {
                Text(
                    text = "● YOU",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = goldColor,
                    fontSize = 9.sp
                )
            }

            // Top team row
            BracketTeamRow(
                scheme = topScheme,
                abbr = series.topTeamAbbr,
                seed = series.topSeed,
                wins = series.topWins,
                isWinner = winner == series.topTeamAbbr,
                isUserSide = series.topSeed == userSeed && isUserSeries
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Bottom team row
            BracketTeamRow(
                scheme = botScheme,
                abbr = series.bottomTeamAbbr,
                seed = series.bottomSeed,
                wins = series.bottomWins,
                isWinner = winner == series.bottomTeamAbbr,
                isUserSide = series.bottomSeed == userSeed && isUserSeries
            )

            // CPU series: tap to pick winner
            if (!isUserSeries && !series.isFinished) {
                TextButton(
                    onClick = { showWinnerPicker = true },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Set Winner",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showWinnerPicker) {
        AlertDialog(
            onDismissRequest = { showWinnerPicker = false },
            title = { Text("Who won this series?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        series.topTeamAbbr to topScheme,
                        series.bottomTeamAbbr to botScheme
                    ).forEach { (abbr, scheme) ->
                        Surface(
                            onClick = {
                                onSetCpuWinner(series.id, abbr)
                                showWinnerPicker = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, scheme.primaryColor.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TeamColorPill(scheme = scheme, size = 24.dp)
                                Text(text = scheme.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWinnerPicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BracketTeamRow(
    scheme: NbaTeamScheme,
    abbr: String,
    seed: Int,
    wins: Int,
    isWinner: Boolean,
    isUserSide: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$seed",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.width(12.dp),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier.size(16.dp).clip(CircleShape).background(scheme.primaryColor),
            contentAlignment = Alignment.Center
        ) {
            Text(text = abbr.take(1), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = scheme.numberColor)
        }
        Text(
            text = abbr,
            fontWeight = if (isWinner || isUserSide) FontWeight.ExtraBold else FontWeight.Medium,
            fontSize = 12.sp,
            color = bracketTeamNameColor(scheme, isWinner, isUserSide, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = wins.toString(),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = if (isWinner) scheme.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Returns a high-contrast text color for a team abbreviation so dark teams (e.g. MIA maroon,
 *  BKN black) stay legible on dark bracket cards. */
private fun bracketTeamNameColor(scheme: NbaTeamScheme, isWinner: Boolean, isUserSide: Boolean, primaryColor: Color, onSurfaceColor: Color): Color {
    val winnerOrUser = if (isWinner) scheme.primaryColor else primaryColor
    return if (isLuminanceDark(winnerOrUser)) Color.White else onSurfaceColor
}

/** Estimates relative luminance per W3C and returns true if the color is dark. */
private fun isLuminanceDark(color: Color): Boolean {
    val r = color.red.toDouble()
    val g = color.green.toDouble()
    val b = color.blue.toDouble()
    val lin = fun(channel: Double): Double =
        if (channel <= 0.03928) channel / 12.92 else Math.pow((channel + 0.055) / 1.055, 2.4)
    val luminance = 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)
    return luminance < 0.5
}

@Composable
private fun BracketConnectorColumn(count: Int) {
    Box(
        modifier = Modifier.width(12.dp).fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight(0.6f)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Series Won Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SeriesWonDialog(
    nextRoundName: String,
    bracket: PlayoffBracketState,
    onViewBracket: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🏆", fontSize = 48.sp)
                Text(
                    text = "SERIES WON!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = Color(0xFFFFD700)
                )
                Text(
                    text = "Advancing to $nextRoundName",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onDismiss(); onViewBracket() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View Bracket")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. NBA Champions Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NbaChampionsDialog(
    bracket: PlayoffBracketState,
    playoffGamesWon: Int,
    playoffGamesLost: Int,
    currentSeason: Int,
    playoffLogs: List<com.example.playerlock.data.GameLog>,
    onClaimFinalsMvp: () -> Unit,
    onAdvance: () -> Unit,
    mvpAlreadyClaimed: Boolean = false
) {
    val goldColor = Color(0xFFFFD700)
    val ppg = if (playoffLogs.isNotEmpty()) playoffLogs.sumOf { it.points }.toDouble() / playoffLogs.size else 0.0
    val rpg = if (playoffLogs.isNotEmpty()) playoffLogs.sumOf { it.rebounds }.toDouble() / playoffLogs.size else 0.0
    val apg = if (playoffLogs.isNotEmpty()) playoffLogs.sumOf { it.assists }.toDouble() / playoffLogs.size else 0.0
    val fgm = playoffLogs.sumOf { it.fieldGoalsMade }
    val fga = playoffLogs.sumOf { it.fieldGoalsAttempted }
    val fgPct = if (fga > 0) fgm * 100.0 / fga else 0.0

    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false)) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, goldColor)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("🏆🎊🏆", fontSize = 48.sp)
                Text(
                    text = "NBA CHAMPIONS!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = goldColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Season $currentSeason  •  $playoffGamesWon-$playoffGamesLost in Playoffs",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Playoff Averages
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = goldColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, goldColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "PLAYOFF AVERAGES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = goldColor,
                            letterSpacing = 1.sp
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf(
                                "PPG" to String.format(Locale.US, "%.1f", ppg),
                                "RPG" to String.format(Locale.US, "%.1f", rpg),
                                "APG" to String.format(Locale.US, "%.1f", apg),
                                "FG%" to String.format(Locale.US, "%.1f%%", fgPct)
                            ).forEach { (label, value) ->
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Claim Finals MVP
                if (!mvpAlreadyClaimed) {
                    Button(
                        onClick = onClaimFinalsMvp,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = goldColor, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Claim Finals MVP (+500 XP)", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }

                // Advance
                Button(
                    onClick = onAdvance,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Advance to Season ${currentSeason + 1} ➔", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Playoff Run Concluded (Elimination) Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlayoffEliminationDialog(
    roundName: String,
    seriesWins: Int,
    seriesLosses: Int,
    currentSeason: Int,
    playoffLogs: List<com.example.playerlock.data.GameLog>,
    onAdvance: (playoffResult: String) -> Unit,
    onDismiss: () -> Unit
) {
    val ppg  = if (playoffLogs.isNotEmpty()) playoffLogs.sumOf { it.points }.toDouble()   / playoffLogs.size else 0.0
    val rpg  = if (playoffLogs.isNotEmpty()) playoffLogs.sumOf { it.rebounds }.toDouble()  / playoffLogs.size else 0.0
    val apg  = if (playoffLogs.isNotEmpty()) playoffLogs.sumOf { it.assists }.toDouble()   / playoffLogs.size else 0.0
    val fgm  = playoffLogs.sumOf { it.fieldGoalsMade }
    val fga  = playoffLogs.sumOf { it.fieldGoalsAttempted }
    val fgPct = if (fga > 0) fgm * 100.0 / fga else 0.0

    val totalW = playoffLogs.count { it.isWin }
    val totalL = playoffLogs.size - totalW
    val playoffResult = "Eliminated in $roundName ($totalW-$totalL)"

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("😔", fontSize = 44.sp)
                Text(
                    text = "PLAYOFF RUN CONCLUDED",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Eliminated in $roundName  ($seriesWins–$seriesLosses in series)",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Playoff averages
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "PLAYOFF AVERAGES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf(
                                "PPG" to String.format(Locale.US, "%.1f", ppg),
                                "RPG" to String.format(Locale.US, "%.1f", rpg),
                                "APG" to String.format(Locale.US, "%.1f", apg),
                                "FG%" to String.format(Locale.US, "%.1f%%", fgPct)
                            ).forEach { (label, value) ->
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { onAdvance(playoffResult) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Advance to Season ${currentSeason + 1} ➔", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TeamColorPill(scheme: NbaTeamScheme, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(scheme.primaryColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = scheme.abbreviation.take(2),
            fontSize = (size.value * 0.28f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = scheme.numberColor,
            textAlign = TextAlign.Center
        )
    }
}
