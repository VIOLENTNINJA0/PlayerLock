package com.example.playerlock.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.playerlock.data.PlayerAward
import com.example.playerlock.data.PlayoffBracketState
import com.example.playerlock.data.ScannedBoxScore
import com.example.playerlock.data.SeasonSummary
import com.example.playerlock.progression.ProgressionCalculator
import kotlinx.coroutines.launch
import java.util.Locale

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun LogGameTab(
    viewModel: PlayerLockViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile
    val scope = rememberCoroutineScope()
    var selectedSubTab by remember { mutableIntStateOf(0) }

    var activeRecapSummary by remember { mutableStateOf<SeasonSummary?>(null) }
    var showRecapDialog by remember { mutableStateOf(false) }
    var showBracketSetup by remember { mutableStateOf(false) }
    var showBracketSheet by remember { mutableStateOf(false) }

    // Playoff series events
    val playoffEvent by viewModel.playoffSeriesEvent.collectAsState()
    var showSeriesWonDialog by remember { mutableStateOf(false) }
    var seriesWonNextRound by remember { mutableStateOf("") }
    var showChampionsDialog by remember { mutableStateOf(false) }
    var champGamesWon by remember { mutableIntStateOf(0) }
    var champGamesLost by remember { mutableIntStateOf(0) }
    var showEliminationDialog by remember { mutableStateOf(false) }
    var eliminationRoundName by remember { mutableStateOf("") }
    var eliminationSeriesWins by remember { mutableIntStateOf(0) }
    var eliminationSeriesLosses by remember { mutableIntStateOf(0) }
    var mvpAlreadyClaimed by remember { mutableStateOf(false) }

    // Observe playoff series events
    LaunchedEffect(playoffEvent) {
        when (val event = playoffEvent) {
            is PlayerLockViewModel.PlayoffSeriesEvent.SeriesWon -> {
                seriesWonNextRound = event.nextRoundName
                showSeriesWonDialog = true
                viewModel.clearPlayoffSeriesEvent()
            }
            is PlayerLockViewModel.PlayoffSeriesEvent.NbaChampion -> {
                champGamesWon = event.playoffGamesWon
                champGamesLost = event.playoffGamesLost
                mvpAlreadyClaimed = false
                showChampionsDialog = true
                viewModel.clearPlayoffSeriesEvent()
            }
            is PlayerLockViewModel.PlayoffSeriesEvent.Eliminated -> {
                eliminationRoundName = event.roundName
                eliminationSeriesWins = event.seriesWins
                eliminationSeriesLosses = event.seriesLosses
                showEliminationDialog = true
                viewModel.clearPlayoffSeriesEvent()
            }
            null -> {}
        }
    }

    val openRecapDialog = {
        if (profile != null) {
            scope.launch {
                val summary = viewModel.buildSeasonSummary(profile.id, profile.currentSeason)
                activeRecapSummary = summary
                showRecapDialog = true
            }
        }
    }

    val bracket = remember(profile?.playoffBracketJson) {
        PlayoffBracketState.fromJson(profile?.playoffBracketJson)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Text(
            text = "LOG GAME",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp
        )

        if (profile?.isInPlayoffs == true) {
            if (bracket != null) {
                // Full playoff series banner with live score
                PlayoffSeriesBanner(
                    bracket = bracket,
                    userTeamName = profile.teamName,
                    onViewBracket = { showBracketSheet = true }
                )
            } else {
                // Bracket not set yet — show setup prompt
                Surface(
                    onClick = { showBracketSetup = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🏆", fontSize = 16.sp)
                            Text(
                                text = "Playoffs Mode • Set Up Bracket",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(text = "Setup →", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // ── 44.dp Segmented Pill Switcher ─────────────────────────────────────
        SegmentedPillSwitcher(
            options = listOf("Game Box Score", "Awards & Honors"),
            selectedIndex = selectedSubTab,
            onOptionSelected = { selectedSubTab = it },
            modifier = Modifier.fillMaxWidth()
        )

        if (selectedSubTab == 0) {
            GameBoxScoreContent(
                viewModel = viewModel,
                onTriggerSeasonConclusion = { openRecapDialog() },
                isInPlayoffs = profile?.isInPlayoffs == true && bracket != null
            )
        } else {
            AwardsAndHonorsContent(viewModel = viewModel)
        }
    }

    // Season Recap Dialog (shown when tapping "Conclude Season" from non-playoff mode)
    if (showRecapDialog && activeRecapSummary != null) {
        SeasonRecapDialog(
            summary = activeRecapSummary!!,
            isCurrentSeasonConclusion = true,
            onContinueToPlayoffs = {
                viewModel.continueToPlayoffs {
                    showRecapDialog = false
                    activeRecapSummary = null
                    showBracketSetup = true
                }
            },
            onAdvanceToNextSeason = {
                viewModel.advanceToNextSeason {
                    showRecapDialog = false
                    activeRecapSummary = null
                }
            },
            onDismiss = {
                showRecapDialog = false
                activeRecapSummary = null
            }
        )
    }

    // Bracket Setup Dialog
    if (showBracketSetup && profile != null) {
        val conf = com.example.playerlock.data.NbaTeamCatalog.getScheme(profile.teamName).conference
        PlayoffBracketSetupDialog(
            userTeamName = profile.teamName,
            userTeamConference = conf,
            onTipOff = { newBracket ->
                viewModel.startPlayoffs(newBracket) { showBracketSetup = false }
            },
            onDismiss = { showBracketSetup = false }
        )
    }

    // Bracket Sheet
    if (showBracketSheet && bracket != null && profile != null) {
        PlayoffBracketSheet(
            bracket = bracket,
            userSeed = bracket.userSeed,
            onSetCpuWinner = { seriesId, winnerAbbr -> viewModel.setCpuSeriesWinner(seriesId, winnerAbbr) },
            onCompleteRemainingCpuSeries = { viewModel.completeRemainingCpuSeries() },
            onDismiss = { showBracketSheet = false }
        )
    }

    // Series Won Dialog
    if (showSeriesWonDialog) {
        SeriesWonDialog(
            nextRoundName = seriesWonNextRound,
            bracket = bracket ?: PlayoffBracketState(),
            onViewBracket = { showBracketSheet = true },
            onDismiss = { showSeriesWonDialog = false }
        )
    }

    // NBA Champions Dialog
    if (showChampionsDialog && profile != null) {
        val playoffLogs = uiState.gameLogs.filter { it.seasonNumber == profile.currentSeason && it.isPlayoff }
        NbaChampionsDialog(
            bracket = bracket ?: PlayoffBracketState(),
            playoffGamesWon = champGamesWon,
            playoffGamesLost = champGamesLost,
            currentSeason = profile.currentSeason,
            playoffLogs = playoffLogs,
            mvpAlreadyClaimed = mvpAlreadyClaimed,
            onClaimFinalsMvp = {
                viewModel.claimAward("Finals MVP", "MAJOR", profile.currentSeason, 500)
                mvpAlreadyClaimed = true
            },
            onAdvance = {
                val w = champGamesWon
                val l = champGamesLost
                viewModel.advanceToNextSeason(playoffResult = "NBA Champions ($w-$l)") {
                    showChampionsDialog = false
                }
            }
        )
    }

    // Elimination Dialog
    if (showEliminationDialog && profile != null) {
        val playoffLogs = uiState.gameLogs.filter { it.seasonNumber == profile.currentSeason && it.isPlayoff }
        PlayoffEliminationDialog(
            roundName = eliminationRoundName,
            seriesWins = eliminationSeriesWins,
            seriesLosses = eliminationSeriesLosses,
            currentSeason = profile.currentSeason,
            playoffLogs = playoffLogs,
            onAdvance = { result ->
                viewModel.advanceToNextSeason(playoffResult = result) {
                    showEliminationDialog = false
                }
            },
            onDismiss = { showEliminationDialog = false }
        )
    }
}

// ---------------------------------------------------------------------------
// 1. Game Box Score Sub-Tab Content
// ---------------------------------------------------------------------------

@Composable
private fun GameBoxScoreContent(
    viewModel: PlayerLockViewModel,
    onTriggerSeasonConclusion: () -> Unit,
    isInPlayoffs: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val quarterLength = uiState.appSettings.quarterLengthMinutes
    val normalizeXp = uiState.appSettings.normalizeQuarterXp

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant

    var minutesText by remember { mutableStateOf("") }
    var points by remember { mutableStateOf("") }
    var rebounds by remember { mutableStateOf("") }
    var assists by remember { mutableStateOf("") }
    var steals by remember { mutableStateOf("") }
    var blocks by remember { mutableStateOf("") }
    var turnovers by remember { mutableStateOf("") }
    var fgmText by remember { mutableStateOf("") }
    var fgaText by remember { mutableStateOf("") }
    var ftmText by remember { mutableStateOf("") }
    var ftaText by remember { mutableStateOf("") }
    var isWin by remember { mutableStateOf<Boolean?>(null) }
    var showResultError by remember { mutableStateOf(false) }
    var pointsValidationError by remember { mutableStateOf<String?>(null) }
    val isPlayoff = isInPlayoffs
    var showScannerDialog by remember { mutableStateOf(false) }
    var scanSuccessBanner by remember { mutableStateOf<String?>(null) }
    var showGame82WarningDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScannerDialog = true
        }
    }

    if (showScannerDialog) {
        BoxScoreScannerDialog(
            onDismiss = { showScannerDialog = false },
            onScanSuccess = { scanned ->
                minutesText = if (scanned.min > 0) scanned.min.toString() else ""
                points = scanned.pts.toString()
                rebounds = scanned.reb.toString()
                assists = scanned.ast.toString()
                steals = scanned.stl.toString()
                blocks = scanned.blk.toString()
                turnovers = scanned.tov.toString()
                fgmText = scanned.fgm.toString()
                fgaText = scanned.fga.toString()
                if (scanned.fta == 0) {
                    ftmText = ""
                    ftaText = ""
                } else {
                    ftmText = scanned.ftm.toString()
                    ftaText = scanned.fta.toString()
                }
                pointsValidationError = null
                showResultError = false
                scanSuccessBanner = "Row scanned — verify stats & tap W or L before saving."
            }
        )
    }



    // Auto-detection of Double-Double and Triple-Double
    val pts = points.toIntOrNull() ?: 0
    val reb = rebounds.toIntOrNull() ?: 0
    val ast = assists.toIntOrNull() ?: 0
    val stl = steals.toIntOrNull() ?: 0
    val blk = blocks.toIntOrNull() ?: 0
    val tov = turnovers.toIntOrNull() ?: 0

    val fgmRaw = fgmText.toIntOrNull() ?: 0
    val fgaRaw = fgaText.toIntOrNull() ?: 0
    val ftmRaw = ftmText.toIntOrNull() ?: 0
    val ftaRaw = ftaText.toIntOrNull() ?: 0

    val fgmInt = fgmRaw
    val fgaInt = if (fgaRaw > 0) fgaRaw.coerceAtLeast(fgmRaw) else fgmRaw
    val ftmInt = ftmRaw
    val ftaInt = if (ftaRaw > 0) ftaRaw.coerceAtLeast(ftmRaw) else ftmRaw

    val fgPct = if (fgaInt > 0) (fgmInt.toDouble() / fgaInt * 100.0).coerceIn(0.0, 100.0) else 0.0

    val doubleDigitCount = listOf(pts, reb, ast, stl, blk).count { it >= 10 }
    val isTripleDouble = doubleDigitCount >= 3
    val isDoubleDouble = doubleDigitCount == 2

    // Auto-calculated performance grade from box score
    val autoPerformanceGrade = remember(pts, reb, ast, stl, blk, tov, fgaInt, fgmInt) {
        ProgressionCalculator.calculatePerformanceGrade(
            points = pts,
            rebounds = reb,
            assists = ast,
            steals = stl,
            blocks = blk,
            turnovers = tov,
            fieldGoalsAttempted = fgaInt,
            fieldGoalsMade = fgmInt
        )
    }

    val totalGamesPlayed = uiState.gameLogs.size
    val activeQuotePair = remember(autoPerformanceGrade, pts, reb, ast, totalGamesPlayed) {
        val quotes = when (autoPerformanceGrade) {
            "A+" -> listOf(
                "You can't stop him, you can only hope to contain him." to "Dan Patrick",
                "Greatness is not this wonderful, esoteric, illusive, god-like feature that only the special among us will ever taste—it's something that truly exists in all of us." to "Kevin Durant",
                "I'm going to use all my tools, my God-given ability, and make the best life I can with it." to "LeBron James",
                "Once you know what failure feels like, determination chases success." to "Kobe Bryant"
            )
            "A", "A-" -> listOf(
                "Excellence is not a singular act, it's a habit. You are what you repeatedly do." to "Shaquille O'Neal",
                "The strength of the team is each individual member. The strength of each member is the team." to "Phil Jackson",
                "Success is not an accident, success is actually a choice." to "Stephen Curry",
                "Be more concerned with your character than your reputation." to "John Wooden"
            )
            "B+", "B", "B-" -> listOf(
                "Ask not what your teammates can do for you. Ask what you can do for your teammates." to "Magic Johnson",
                "Good, better, best. Never let it rest. Until your good is better and your better is best." to "Tim Duncan",
                "Push yourself again and again. Don't give an inch until the final buzzer sounds." to "Larry Bird",
                "What you are as a person is far more important than what you are as a basketball player." to "John Wooden"
            )
            "C+", "C" -> listOf(
                "You miss 100% of the shots you don't take." to "Wayne Gretzky",
                "Everything negative—pressure, challenges—is all an opportunity for me to rise." to "Kobe Bryant",
                "A winner is someone who recognizes his God-given talents, works his tail off to develop them into skills, and uses these skills to accomplish his goals." to "Larry Bird",
                "The key is not the will to win. Everybody has that. It is the will to prepare to win that is important." to "Bobby Knight"
            )
            else -> listOf(
                "I've missed more than 9,000 shots in my career. I've lost almost 300 games. I've failed over and over and over again in my life. And that is why I succeed." to "Michael Jordan",
                "If you're afraid to fail, then you're probably going to fail." to "Kobe Bryant",
                "Failure happens all the time. It happens every day in practice. What makes you better is how you react to it." to "Mia Hamm",
                "One man can be a crucial ingredient on a team, but one man cannot make a team." to "Kareem Abdul-Jabbar"
            )
        }
        val seed = kotlin.math.abs(pts + reb + ast + totalGamesPlayed)
        quotes[seed % quotes.size]
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ── Card 1: Box Score ────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
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
                        text = "BOX SCORE",
                        style = MaterialTheme.typography.labelMedium,
                        color = onSurfaceVarColor,
                        letterSpacing = 1.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (fgaInt > 0) {
                            val fgPctStr = String.format(Locale.US, "%.1f", fgPct)
                            Text(
                                text = "FG: $fgPctStr%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor.copy(alpha = 0.85f)
                            )
                        }

                        Surface(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    showScannerDialog = true
                                } else {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = primaryColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.30f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = "Scan Row",
                                    tint = primaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Scan Row",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoxScoreInput(label = "MIN", value = minutesText, onValueChange = { minutesText = it.filter(Char::isDigit).take(3); pointsValidationError = null }, modifier = Modifier.weight(1f))
                    BoxScoreInput(label = "PTS", value = points, onValueChange = { points = it.filter(Char::isDigit).take(3); pointsValidationError = null }, modifier = Modifier.weight(1f))
                    BoxScoreInput(label = "REB", value = rebounds, onValueChange = { rebounds = it.filter(Char::isDigit).take(3); pointsValidationError = null }, modifier = Modifier.weight(1f))
                    BoxScoreInput(label = "AST", value = assists, onValueChange = { assists = it.filter(Char::isDigit).take(3); pointsValidationError = null }, modifier = Modifier.weight(1f))
                    BoxScoreInput(label = "STL", value = steals, onValueChange = { steals = it.filter(Char::isDigit).take(3); pointsValidationError = null }, modifier = Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoxScoreInput(label = "BLK", value = blocks, onValueChange = { blocks = it.filter(Char::isDigit).take(3); pointsValidationError = null }, modifier = Modifier.weight(1f))
                    BoxScoreInput(label = "TOV", value = turnovers, onValueChange = { turnovers = it.filter(Char::isDigit).take(3); pointsValidationError = null }, modifier = Modifier.weight(1f))
                    SplitStatInput(
                        label = "FG",
                        madeValue = fgmText,
                        onMadeChange = { fgmText = it.filter(Char::isDigit).take(2); pointsValidationError = null },
                        attemptedValue = fgaText,
                        onAttemptedChange = { fgaText = it.filter(Char::isDigit).take(2); pointsValidationError = null },
                        modifier = Modifier.weight(1.2f)
                    )
                    SplitStatInput(
                        label = "FT",
                        madeValue = ftmText,
                        onMadeChange = { ftmText = it.filter(Char::isDigit).take(2); pointsValidationError = null },
                        attemptedValue = ftaText,
                        onAttemptedChange = { ftaText = it.filter(Char::isDigit).take(2); pointsValidationError = null },
                        modifier = Modifier.weight(1.2f)
                    )
                }

                scanSuccessBanner?.let { bannerText ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = primaryColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = bannerText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // ── Card 2: Result & Automatic Performance Rating ─────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "GAME RESULT",
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceVarColor,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // W button
                    Surface(
                        onClick = {
                            isWin = true
                            showResultError = false
                            pointsValidationError = null
                        },
                        shape = RoundedCornerShape(50),
                        color = if (isWin == true) Color(0xFF10B981) else surfaceVarColor,
                        border = if (isWin == true) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "W",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isWin == true) Color.Black else onSurfaceVarColor
                            )
                        }
                    }

                    // L button
                    Surface(
                        onClick = {
                            isWin = false
                            showResultError = false
                        },
                        shape = RoundedCornerShape(50),
                        color = if (isWin == false) MaterialTheme.colorScheme.error else surfaceVarColor,
                        border = if (isWin == false) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "L",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isWin == false) Color.White else onSurfaceVarColor
                            )
                        }
                    }

                    // Playoff chip (Read-only: ON in playoffs, OFF in regular season)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isInPlayoffs) primaryColor else surfaceVarColor.copy(alpha = 0.45f),
                        border = if (isInPlayoffs) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(40.dp)
                            .alpha(if (isInPlayoffs) 1f else 0.45f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isInPlayoffs) "🏆 Playoff Game" else "Playoff Game",
                                fontWeight = if (isInPlayoffs) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isInPlayoffs) onPrimaryColor else onSurfaceVarColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PERFORMANCE RATING",
                        style = MaterialTheme.typography.labelMedium,
                        color = onSurfaceVarColor,
                        letterSpacing = 1.sp
                    )
                    val gradeMult = when (autoPerformanceGrade) {
                        "A+" -> "1.25x MULTIPLIER"
                        "A", "A-" -> "1.15x MULTIPLIER"
                        "B+", "B", "B-" -> "1.00x MULTIPLIER"
                        else -> "0.85x MULTIPLIER"
                    }
                    Text(
                        text = gradeMult,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor.copy(alpha = 0.85f),
                        letterSpacing = 0.5.sp
                    )
                }

                val grades = listOf("A+", "A", "A-", "B+", "B", "B-", "C+", "C", "D/F")

                // Horizontal Row of Distinct Color-Coded Grade Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grades.forEach { grade ->
                        val selected = autoPerformanceGrade == grade
                        val tierColor = when (grade) {
                            "A+" -> Color(0xFFFFD700)
                            "A", "A-" -> Color(0xFF10B981)
                            "B+", "B", "B-" -> Color(0xFF3B82F6)
                            "C+", "C" -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        }

                        val chipBg = if (selected) tierColor else surfaceVarColor
                        val chipBorder = if (selected) BorderStroke(1.dp, tierColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        val textColor = when {
                            selected && (grade == "A+" || grade == "C+" || grade == "C") -> Color.Black
                            selected -> Color.White
                            else -> onSurfaceVarColor.copy(alpha = 0.6f)
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = chipBg,
                            border = chipBorder
                        ) {
                            Text(
                                text = grade,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 14.sp,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Attributed Quote Banner below the grade row
                val bannerTierColor = when (autoPerformanceGrade) {
                    "A+" -> Color(0xFFFFD700)
                    "A", "A-" -> Color(0xFF10B981)
                    "B+", "B", "B-" -> Color(0xFF3B82F6)
                    "C+", "C" -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = bannerTierColor.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, bannerTierColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "\"${activeQuotePair.first}\"",
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp,
                            color = onSurfaceColor,
                            lineHeight = 17.sp
                        )
                        Text(
                            text = "— ${activeQuotePair.second}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = bannerTierColor,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        // ── Card 3: Auto-Detected Stat Milestones ─────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        text = "STAT MILESTONES",
                        style = MaterialTheme.typography.labelMedium,
                        color = onSurfaceVarColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "AUTO-CALCULATED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor.copy(alpha = 0.85f),
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AutoMilestoneStatusBadge(
                        name = "Double-Double",
                        xpBonus = 15,
                        isActive = isDoubleDouble,
                        modifier = Modifier.weight(1f)
                    )
                    AutoMilestoneStatusBadge(
                        name = "Triple-Double",
                        xpBonus = 35,
                        isActive = isTripleDouble,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (showResultError) {
            Text(
                text = "Please select whether the game was a Win (W) or Loss (L).",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        pointsValidationError?.let { errorMsg ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "⚠️ ",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ── Action Buttons ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.calculateXpPreview(
                        points = pts,
                        rebounds = reb,
                        assists = ast,
                        steals = stl,
                        blocks = blk,
                        turnovers = tov,
                        fieldGoalsAttempted = fgaInt,
                        fieldGoalsMade = fgmInt,
                        isWin = isWin ?: false,
                        isPlayoff = isPlayoff,
                        teammateGrade = autoPerformanceGrade,
                        quarterMinutes = quarterLength,
                        useQuarterNormalizer = normalizeXp,
                        isDoubleDouble = isDoubleDouble,
                        isTripleDouble = isTripleDouble,
                        isPlayerOfTheGame = false,
                        isPlayerOfTheWeek = false,
                        isAllStar = false,
                        isMajorAward = false
                    )
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, primaryColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Preview XP", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Button(
                onClick = {
                    var hasError = false
                    val currentIsWin = isWin
                    if (currentIsWin == null) {
                        showResultError = true
                        hasError = true
                    } else {
                        showResultError = false
                    }

                    if ((fgaText.isNotEmpty() && fgmRaw > fgaRaw) || (ftaText.isNotEmpty() && ftmRaw > ftaRaw)) {
                        pointsValidationError = "Made shots cannot exceed attempted shots."
                        hasError = true
                    } else if (pts > 0 && fgaInt == 0 && ftaInt == 0) {
                        pointsValidationError = "Please enter your FG (FGM/FGA) before saving a scoring game."
                        hasError = true
                    } else {
                        val minPointsRequired = (fgmInt * 2) + ftmInt
                        if (pts < minPointsRequired) {
                            pointsValidationError = "Points ($pts) cannot be less than $minPointsRequired for $fgmInt FGM and $ftmInt FTM."
                            hasError = true
                        } else {
                            val maxPossiblePts = (fgmInt * 3) + ftmInt
                            if (pts > maxPossiblePts) {
                                pointsValidationError = "Points ($pts) exceed the maximum possible ($maxPossiblePts) for $fgmInt FGM and $ftmInt FTM."
                                hasError = true
                            } else {
                                pointsValidationError = null
                            }
                        }
                    }

                    if (hasError) return@Button

                    val executeSave = {
                        val minVal = minutesText.toIntOrNull() ?: 0
                        viewModel.logGame(
                            minutesPlayed = minVal,
                            points = pts,
                            rebounds = reb,
                            assists = ast,
                            steals = stl,
                            blocks = blk,
                            turnovers = tov,
                            fieldGoalsAttempted = fgaInt,
                            fieldGoalsMade = fgmInt,
                            freeThrowsAttempted = ftaInt,
                            freeThrowsMade = ftmInt,
                            isWin = currentIsWin!!,
                            isPlayoff = isPlayoff,
                            teammateGrade = autoPerformanceGrade,
                            quarterMinutes = quarterLength,
                            useQuarterNormalizer = normalizeXp,
                            isDoubleDouble = isDoubleDouble,
                            isTripleDouble = isTripleDouble,
                            isPlayerOfTheGame = false,
                            isPlayerOfTheWeek = false,
                            isAllStar = false,
                            isMajorAward = false
                        )

                        if (!isPlayoff) {
                            val latestUiState = viewModel.uiState.value
                            val latestProfile = latestUiState.profile
                            if (latestProfile != null && !latestProfile.isInPlayoffs) {
                                val nonPlayoffCount = latestUiState.gameLogs.count { it.seasonNumber == latestProfile.currentSeason && !it.isPlayoff }
                                if (nonPlayoffCount >= 82) {
                                    onTriggerSeasonConclusion()
                                }
                            }
                        }
                    }

                    val currentProfile = uiState.profile
                    val regularSeasonGamesCount = uiState.gameLogs.count {
                        it.seasonNumber == (currentProfile?.currentSeason ?: 1) && !it.isPlayoff
                    }
                    val isGame82 = !isPlayoff && currentProfile != null && !currentProfile.isInPlayoffs && regularSeasonGamesCount == 81

                    if (isGame82) {
                        showGame82WarningDialog = true
                    } else {
                        executeSave()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = onPrimaryColor
                ),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Save Game", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // ── Preview Card ─────────────────────────────────────────────────────
        val preview by viewModel.xpPreview.collectAsState()
        preview?.let { XpPreviewCard(it) }

        if (showGame82WarningDialog) {
            AlertDialog(
                onDismissRequest = { showGame82WarningDialog = false },
                title = {
                    Text(
                        text = "Conclude Regular Season (Game 82)?",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "Proceeding with this will make you unable to delete past games from History, refund spent points, or reverse given awards.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showGame82WarningDialog = false
                            val currentIsWin = isWin ?: return@Button
                            val minVal = minutesText.toIntOrNull() ?: 0
                            viewModel.logGame(
                                minutesPlayed = minVal,
                                points = pts,
                                rebounds = reb,
                                assists = ast,
                                steals = stl,
                                blocks = blk,
                                turnovers = tov,
                                fieldGoalsAttempted = fgaInt,
                                fieldGoalsMade = fgmInt,
                                freeThrowsAttempted = ftaInt,
                                freeThrowsMade = ftmInt,
                                isWin = currentIsWin,
                                isPlayoff = isPlayoff,
                                teammateGrade = autoPerformanceGrade,
                                quarterMinutes = quarterLength,
                                useQuarterNormalizer = normalizeXp,
                                isDoubleDouble = isDoubleDouble,
                                isTripleDouble = isTripleDouble,
                                isPlayerOfTheGame = false,
                                isPlayerOfTheWeek = false,
                                isAllStar = false,
                                isMajorAward = false
                            )

                            if (!isPlayoff) {
                                val latestUiState = viewModel.uiState.value
                                val latestProfile = latestUiState.profile
                                if (latestProfile != null && !latestProfile.isInPlayoffs) {
                                    val nonPlayoffCount = latestUiState.gameLogs.count {
                                        it.seasonNumber == latestProfile.currentSeason && !it.isPlayoff
                                    }
                                    if (nonPlayoffCount >= 82) {
                                        onTriggerSeasonConclusion()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = onPrimaryColor
                        )
                    ) {
                        Text("Proceed & Log Game 82", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showGame82WarningDialog = false }
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ---------------------------------------------------------------------------
// 2. Awards & Honors Sub-Tab Content
// ---------------------------------------------------------------------------

private data class AwardDefinition(
    val name: String,
    val category: String,
    val xpBonus: Int
)

@Composable
private fun AwardsAndHonorsContent(viewModel: PlayerLockViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val awards = uiState.awards
    var seasonNumber by remember { mutableIntStateOf(1) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant

    val weeklyAwards = listOf(
        AwardDefinition("Player of the Game", "WEEKLY", 20),
        AwardDefinition("Player of the Week", "WEEKLY", 50)
    )

    val allStarAwards = listOf(
        AwardDefinition("All-Star", "ALL_STAR", 150)
    )

    val allNbaAwards = listOf(
        AwardDefinition("All-NBA 1st Team", "ALL_NBA", 250),
        AwardDefinition("All-NBA 2nd Team", "ALL_NBA", 250),
        AwardDefinition("All-NBA 3rd Team", "ALL_NBA", 250)
    )

    val allDefenseAwards = listOf(
        AwardDefinition("All-Defensive 1st Team", "ALL_DEFENSE", 200),
        AwardDefinition("All-Defensive 2nd Team", "ALL_DEFENSE", 200)
    )

    val majorAwards = listOf(
        AwardDefinition("MVP", "MAJOR", 500),
        AwardDefinition("Finals MVP", "MAJOR", 500),
        AwardDefinition("Defensive Player of the Year", "MAJOR", 500),
        AwardDefinition("Rookie of the Year", "MAJOR", 500),
        AwardDefinition("Most Improved Player", "MAJOR", 500),
        AwardDefinition("Sixth Man of the Year", "MAJOR", 500)
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── Season Stepper Header ─────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ACTIVE SEASON",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVarColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Season $seasonNumber",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (seasonNumber > 1) seasonNumber-- },
                        enabled = seasonNumber > 1,
                        modifier = Modifier
                            .size(36.dp)
                            .background(surfaceVarColor, CircleShape)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Previous Season", tint = if (seasonNumber > 1) primaryColor else onSurfaceVarColor.copy(alpha = 0.4f))
                    }
                    IconButton(
                        onClick = { if (seasonNumber < 25) seasonNumber++ },
                        enabled = seasonNumber < 25,
                        modifier = Modifier
                            .size(36.dp)
                            .background(surfaceVarColor, CircleShape)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Next Season", tint = if (seasonNumber < 25) primaryColor else onSurfaceVarColor.copy(alpha = 0.4f))
                    }
                }
            }
        }

        // ── Award Categories ──────────────────────────────────────────────────
        AwardSectionCard(
            title = "WEEKLY / IN-GAME HONORS",
            awards = weeklyAwards,
            onClaim = { def ->
                viewModel.claimAward(def.name, def.category, seasonNumber, def.xpBonus)
            }
        )

        AwardSectionCard(
            title = "ALL-STAR SELECTION",
            awards = allStarAwards,
            onClaim = { def ->
                viewModel.claimAward(def.name, def.category, seasonNumber, def.xpBonus)
            }
        )

        AwardSectionCard(
            title = "ALL-NBA TEAMS (+250 XP)",
            awards = allNbaAwards,
            onClaim = { def ->
                viewModel.claimAward(def.name, def.category, seasonNumber, def.xpBonus)
            }
        )

        AwardSectionCard(
            title = "ALL-DEFENSIVE TEAMS (+200 XP)",
            awards = allDefenseAwards,
            onClaim = { def ->
                viewModel.claimAward(def.name, def.category, seasonNumber, def.xpBonus)
            }
        )

        AwardSectionCard(
            title = "MAJOR NBA AWARDS (+500 XP)",
            awards = majorAwards,
            onClaim = { def ->
                viewModel.claimAward(def.name, def.category, seasonNumber, def.xpBonus)
            }
        )

        // ── Claimed Awards List ───────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        text = "CLAIMED AWARDS HISTORY",
                        style = MaterialTheme.typography.labelMedium,
                        color = onSurfaceVarColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${awards.size} Claimed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }

                if (awards.isEmpty()) {
                    Text(
                        text = "No awards claimed yet for this player career.",
                        fontSize = 12.sp,
                        color = onSurfaceVarColor,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    awards.forEach { award ->
                        ClaimedAwardRow(
                            award = award,
                            currentSeasonNumber = seasonNumber,
                            onDelete = { viewModel.deleteAward(award) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AwardSectionCard(
    title: String,
    awards: List<AwardDefinition>,
    onClaim: (AwardDefinition) -> Unit
) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = onSurfaceVarColor,
                letterSpacing = 1.sp
            )

            awards.forEach { def ->
                Surface(
                    onClick = { onClaim(def) },
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceVarColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = def.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = secondaryColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, secondaryColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "+${def.xpBonus} XP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = secondaryColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClaimedAwardRow(
    award: PlayerAward,
    currentSeasonNumber: Int,
    onDelete: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val isSeasonActive = award.seasonNumber == currentSeasonNumber

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = surfaceVarColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = award.awardName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceColor
                )
                Text(
                    text = "Season ${award.seasonNumber} • +${award.xpGranted} XP",
                    fontSize = 11.sp,
                    color = primaryColor
                )
            }

            if (isSeasonActive) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Undo / Delete Award",
                        tint = onSurfaceVarColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
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
                        tint = onSurfaceVarColor.copy(alpha = 0.45f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Locked",
                        fontSize = 11.sp,
                        color = onSurfaceVarColor.copy(alpha = 0.55f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Shared Helpers & Inputs
// ---------------------------------------------------------------------------

@Composable
private fun AutoMilestoneStatusBadge(
    name: String,
    xpBonus: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val bgColor = if (isActive) secondaryColor.copy(alpha = 0.15f) else surfaceVarColor
    val borderColor = if (isActive) secondaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val contentAlpha = if (isActive) 1.0f else 0.4f
    val textColor = if (isActive) onSurfaceColor else onSurfaceVarColor.copy(alpha = 0.5f)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Earned",
                        tint = secondaryColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isActive) secondaryColor.copy(alpha = 0.25f) else surfaceVarColor,
                border = BorderStroke(1.dp, if (isActive) secondaryColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "+$xpBonus XP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) secondaryColor else onSurfaceVarColor.copy(alpha = contentAlpha),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
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
                            fontSize = 14.sp,
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
private fun SplitStatInput(
    label: String,
    madeValue: String,
    onMadeChange: (String) -> Unit,
    attemptedValue: String,
    onAttemptedChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = onSurfaceVarColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = surfaceVarColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left half: Made input
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (madeValue.isEmpty()) {
                        Text(
                            text = "0",
                            style = TextStyle(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = onSurfaceVarColor.copy(alpha = 0.4f)
                            )
                        )
                    }
                    BasicTextField(
                        value = madeValue,
                        onValueChange = onMadeChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        textStyle = TextStyle(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = onSurfaceColor
                        ),
                        cursorBrush = SolidColor(primaryColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Center static "/" divider
                Text(
                    text = "/",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceVarColor
                )

                // Right half: Attempted input
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (attemptedValue.isEmpty()) {
                        Text(
                            text = "0",
                            style = TextStyle(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = onSurfaceVarColor.copy(alpha = 0.4f)
                            )
                        )
                    }
                    BasicTextField(
                        value = attemptedValue,
                        onValueChange = onAttemptedChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        textStyle = TextStyle(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = onSurfaceColor
                        ),
                        cursorBrush = SolidColor(primaryColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScoreInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = onSurfaceVarColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = placeholder?.let {
                {
                    Text(
                        text = it,
                        style = TextStyle(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = onSurfaceVarColor.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = onSurfaceColor
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                cursorColor = primaryColor,
                focusedContainerColor = surfaceVarColor,
                unfocusedContainerColor = surfaceVarColor
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )
    }
}

@Composable
fun XpPreviewCard(result: ProgressionCalculator.GameXpResult) {
    val apGained = (result.totalXp / 25).toInt()
    val bpGained = (result.totalXp / 100).toInt()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "XP BREAKDOWN PREVIEW",
                style = MaterialTheme.typography.labelMedium,
                color = primaryColor,
                letterSpacing = 1.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total XP", style = MaterialTheme.typography.labelSmall, color = onSurfaceVarColor)
                    Text("+${result.totalXp.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                }
                Column {
                    Text("AP Gained", style = MaterialTheme.typography.labelSmall, color = onSurfaceVarColor)
                    Text("+$apGained AP", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                }
                Column {
                    Text("BP Gained", style = MaterialTheme.typography.labelSmall, color = onSurfaceVarColor)
                    Text("+$bpGained BP", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = secondaryColor)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Win Mult: x${String.format(Locale.US, "%.1f", result.winMultiplier)}", style = MaterialTheme.typography.labelSmall, color = onSurfaceVarColor)
                Text("TG Mult: x${String.format(Locale.US, "%.2f", result.teammateGradeMultiplier)}", style = MaterialTheme.typography.labelSmall, color = onSurfaceVarColor)
                Text("Eff Mult: x${String.format(Locale.US, "%.2f", result.efficiencyMultiplier)}", style = MaterialTheme.typography.labelSmall, color = onSurfaceVarColor)
            }
        }
    }
}
