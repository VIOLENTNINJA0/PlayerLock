package com.example.playerlock.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playerlock.data.AppSettings
import com.example.playerlock.data.CustomPlayerTemplate
import com.example.playerlock.data.GameLog
import com.example.playerlock.data.NbaTeamCatalog
import com.example.playerlock.data.PlayoffBracketState
import com.example.playerlock.data.PlayoffSeries
import com.example.playerlock.data.PlayerAttribute
import com.example.playerlock.data.PlayerAward
import com.example.playerlock.data.PlayerBadge
import com.example.playerlock.data.PlayerProfile
import com.example.playerlock.data.SeasonSummary
import com.example.playerlock.data.SettingsDataStore
import com.example.playerlock.data.UpgradeHistory
import com.example.playerlock.progression.ProgressionCalculator
import com.example.playerlock.progression.ProgressionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerLockUiState(
    val profile: PlayerProfile? = null,
    val attributes: List<PlayerAttribute> = emptyList(),
    val badges: List<PlayerBadge> = emptyList(),
    val gameLogs: List<GameLog> = emptyList(),
    val upgradeHistory: List<UpgradeHistory> = emptyList(),
    val awards: List<PlayerAward> = emptyList(),
    val seasonSummaries: List<SeasonSummary> = emptyList(),
    val appSettings: AppSettings = AppSettings(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showFirstLaunchSetup: Boolean = false,
    val xpPreview: ProgressionCalculator.GameXpResult? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerLockViewModel(
    private val repository: ProgressionRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val backupManager = com.example.playerlock.data.CareerBackupManager(repository, settingsDataStore)

    val allProfiles: StateFlow<List<PlayerProfile>> = repository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfileId: StateFlow<Int?> = settingsDataStore.activeProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeProfile: StateFlow<PlayerProfile?> = activeProfileId.flatMapLatest { id ->
        if (id != null) repository.getProfileFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val attributes: StateFlow<List<PlayerAttribute>> = activeProfileId.flatMapLatest { id ->
        if (id != null) repository.getAllAttributes(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val badges: StateFlow<List<PlayerBadge>> = activeProfileId.flatMapLatest { id ->
        if (id != null) repository.getAllBadgesFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gameLogs: StateFlow<List<GameLog>> = activeProfileId.flatMapLatest { id ->
        if (id != null) repository.getAllGameLogsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upgradeHistory: StateFlow<List<UpgradeHistory>> = activeProfileId.flatMapLatest { id ->
        if (id != null) repository.getAllUpgradeHistoryFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val awards: StateFlow<List<PlayerAward>> = activeProfileId.flatMapLatest { id ->
        if (id != null) repository.getAllAwardsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seasonSummaries: StateFlow<List<SeasonSummary>> = activeProfileId.flatMapLatest { id ->
        if (id != null) repository.getAllSeasonSummariesFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<AppSettings> = settingsDataStore.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<PlayerLockUiState> = combine(
        combine(activeProfile, attributes, badges, gameLogs, upgradeHistory) { profile, attrs, bdgs, logs, history ->
            PlayerLockUiState(
                profile = profile,
                attributes = attrs,
                badges = bdgs,
                gameLogs = logs,
                upgradeHistory = history
            )
        },
        combine(awards, seasonSummaries, appSettings) { awardList, summariesList, settings ->
            Triple(awardList, summariesList, settings)
        },
        combine(_errorMessage, _isLoading) { errorMsg, loading ->
            Pair(errorMsg, loading)
        }
    ) { baseState, (awardList, summariesList, settings), (errorMsg, loading) ->
        baseState.copy(
            awards = awardList,
            seasonSummaries = summariesList,
            appSettings = settings,
            errorMessage = errorMsg,
            isLoading = loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerLockUiState())

    private val _xpPreview = MutableStateFlow<ProgressionCalculator.GameXpResult?>(null)
    val xpPreview: StateFlow<ProgressionCalculator.GameXpResult?> = _xpPreview.asStateFlow()

    init {
        // Whenever the active profile changes, run catalog sync so that existing DB rows
        // get their category and sortOrder updated to match AttributeSeedData (1..36)
        // and BadgeSeedData (1..53) before the Flows emit to the UI.
        viewModelScope.launch {
            activeProfileId.collect { profileId ->
                if (profileId != null) {
                    repository.syncExistingAttributes(profileId)
                    repository.syncExistingBadges(profileId)
                }
            }
        }
    }

    fun selectProfile(profileId: Int) {
        viewModelScope.launch {
            settingsDataStore.setActiveProfileId(profileId)
        }
    }

    fun switchProfile() {
        viewModelScope.launch {
            settingsDataStore.setActiveProfileId(null)
        }
    }

    fun deleteProfile(profileId: Int) {
        viewModelScope.launch {
            if (activeProfileId.value == profileId) {
                settingsDataStore.setActiveProfileId(null)
            }
            repository.deleteProfile(profileId)
        }
    }

    fun createPlayer(
        playerName: String,
        position: String,
        startingOvr: Int,
        archetype: String = "Balanced",
        heightInches: Int = 78,
        weightLbs: Int = 210,
        jerseyNumber: Int = 23,
        teamName: String = "Draft Prospect",
        customAttributes: Map<String, Int>? = null,
        customBadges: Map<String, String>? = null,
        avatarPath: String? = null
    ) {
        viewModelScope.launch {
            val newId = repository.createPlayer(
                playerName = playerName,
                position = position,
                startingOvr = startingOvr,
                archetype = archetype,
                heightInches = heightInches,
                weightLbs = weightLbs,
                jerseyNumber = jerseyNumber,
                teamName = teamName,
                customAttributes = customAttributes,
                customBadges = customBadges,
                avatarPath = avatarPath
            )
            settingsDataStore.setActiveProfileId(newId)
            triggerAutoSaveInternal(newId)
        }
    }

    private fun triggerAutoSaveInternal(profileId: Int? = activeProfileId.value) {
        val id = profileId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            backupManager.performAutoSaveIfEnabled(id)
        }
    }

    fun triggerAutoSave() {
        triggerAutoSaveInternal()
    }

    fun updateAutoSaveEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveAutoSaveEnabled(enabled)
        }
    }

    fun updateAutoSaveFolderUri(uri: String?) {
        viewModelScope.launch {
            settingsDataStore.saveAutoSaveFolderUri(uri)
        }
    }

    fun updateLastAutoSaveTimestamp(timestamp: Long) {
        viewModelScope.launch {
            settingsDataStore.saveLastAutoSaveTimestamp(timestamp)
        }
    }

    fun logGame(
        minutesPlayed: Int = 0,
        points: Int,
        rebounds: Int,
        assists: Int,
        steals: Int,
        blocks: Int,
        turnovers: Int,
        fieldGoalsAttempted: Int,
        fieldGoalsMade: Int,
        freeThrowsAttempted: Int = 0,
        freeThrowsMade: Int = 0,
        isWin: Boolean,
        isPlayoff: Boolean,
        teammateGrade: String,
        quarterMinutes: Int,
        useQuarterNormalizer: Boolean,
        isDoubleDouble: Boolean,
        isTripleDouble: Boolean,
        isPlayerOfTheGame: Boolean,
        isPlayerOfTheWeek: Boolean,
        isAllStar: Boolean,
        isMajorAward: Boolean
    ) {
        viewModelScope.launch {
            val currentProfileId = activeProfileId.value ?: return@launch
            try {
                val result = repository.recordGameAndCalculateProgression(
                    profileId = currentProfileId,
                    minutesPlayed = minutesPlayed,
                    points = points,
                    rebounds = rebounds,
                    assists = assists,
                    steals = steals,
                    blocks = blocks,
                    turnovers = turnovers,
                    fieldGoalsAttempted = fieldGoalsAttempted,
                    fieldGoalsMade = fieldGoalsMade,
                    freeThrowsAttempted = freeThrowsAttempted,
                    freeThrowsMade = freeThrowsMade,
                    isWin = isWin,
                    isPlayoff = isPlayoff,
                    teammateGrade = teammateGrade,
                    quarterMinutes = quarterMinutes,
                    useQuarterNormalizer = useQuarterNormalizer,
                    isDoubleDouble = isDoubleDouble,
                    isTripleDouble = isTripleDouble,
                    isPlayerOfTheGame = isPlayerOfTheGame,
                    isPlayerOfTheWeek = isPlayerOfTheWeek,
                    isAllStar = isAllStar,
                    isMajorAward = isMajorAward
                )
                _xpPreview.value = result

                val currentProfile = repository.getProfile(currentProfileId) ?: return@launch
                if (currentProfile.isInPlayoffs || (isPlayoff && !currentProfile.playoffBracketJson.isNullOrBlank())) {
                    val bracket = PlayoffBracketState.fromJson(currentProfile.playoffBracketJson)
                    if (bracket != null) {
                        // Guaranteed: find the active user series. Prefer activeUserSeries; fallback: match userTeamAbbr
                        // against topTeamAbbr / bottomTeamAbbr within the current round.
                        val userTeamAbbr = bracket.userTeamAbbr.takeIf { it.isNotBlank() }
                            ?: NbaTeamCatalog.getScheme(currentProfile.teamName).abbreviation

                        val userSeries = bracket.activeUserSeries
                            ?: when (bracket.currentRound) {
                                1 -> bracket.round1Matchups.firstOrNull { !it.isFinished && (it.topTeamAbbr == userTeamAbbr || it.bottomTeamAbbr == userTeamAbbr) }
                                2 -> bracket.semisMatchups.firstOrNull { !it.isFinished && (it.topTeamAbbr == userTeamAbbr || it.bottomTeamAbbr == userTeamAbbr) }
                                3 -> listOfNotNull(bracket.confFinalsMatchup).firstOrNull { !it.isFinished && (it.topTeamAbbr == userTeamAbbr || it.bottomTeamAbbr == userTeamAbbr) }
                                4 -> listOfNotNull(bracket.nbaFinalsMatchup).firstOrNull { !it.isFinished && (it.topTeamAbbr == userTeamAbbr || it.bottomTeamAbbr == userTeamAbbr) }
                                else -> null
                            }

                        if (userSeries != null && !userSeries.isFinished) {
                            val userIsTop = userSeries.topTeamAbbr == userTeamAbbr || userSeries.topSeed == bracket.userSeed

                            val updatedSeries = if (userIsTop) {
                                if (isWin) userSeries.copy(topWins = (userSeries.topWins + 1).coerceAtMost(4))
                                else userSeries.copy(bottomWins = (userSeries.bottomWins + 1).coerceAtMost(4))
                            } else {
                                if (isWin) userSeries.copy(bottomWins = (userSeries.bottomWins + 1).coerceAtMost(4))
                                else userSeries.copy(topWins = (userSeries.topWins + 1).coerceAtMost(4))
                            }

                            val userWins = if (userIsTop) updatedSeries.topWins else updatedSeries.bottomWins
                            val oppWins = if (userIsTop) updatedSeries.bottomWins else updatedSeries.topWins
                            val isSeriesFinished = userWins >= 4 || oppWins >= 4
                            val finalSeries = if (isSeriesFinished) updatedSeries.copy(isFinished = true) else updatedSeries

                            val replacedBracket = replaceSeries(bracket, finalSeries)
                            val prevRound = bracket.currentRound
                            val refreshedBracket = PlayoffBracketState.refreshBracket(replacedBracket)

                            // Persist immediately so Dashboard banner, Log Game banner & Bracket update live
                            if (userWins >= 4) {
                                if (prevRound == 4) {
                                    val playoffLogs = repository.getAllGameLogs(currentProfileId)
                                        .filter { it.seasonNumber == currentProfile.currentSeason && it.isPlayoff }
                                    val champWins = playoffLogs.count { it.isWin }
                                    val champLosses = playoffLogs.size - champWins
                                    repository.claimAward(
                                        profileId = currentProfileId,
                                        awardName = "NBA Champion",
                                        category = "MAJOR",
                                        seasonNumber = currentProfile.currentSeason,
                                        xpGranted = 500
                                    )
                                    val finalChampBracket = refreshedBracket.copy(
                                        nbaFinalsMatchup = refreshedBracket.nbaFinalsMatchup?.copy(isFinished = true)
                                    )
                                    repository.updateProfile(currentProfile.copy(playoffBracketJson = finalChampBracket.toJson()))
                                    _playoffSeriesEvent.value = PlayoffSeriesEvent.NbaChampion(finalChampBracket, champWins, champLosses)
                                } else {
                                    repository.updateProfile(currentProfile.copy(playoffBracketJson = refreshedBracket.toJson()))
                                    _playoffSeriesEvent.value = PlayoffSeriesEvent.SeriesWon(refreshedBracket.currentRoundName, refreshedBracket)
                                }
                            } else if (oppWins >= 4) {
                                repository.updateProfile(currentProfile.copy(
                                    playoffBracketJson = refreshedBracket.toJson(),
                                    isInPlayoffs = false
                                ))
                                _playoffSeriesEvent.value = PlayoffSeriesEvent.Eliminated(
                                    bracket.currentRoundName, userWins, oppWins, refreshedBracket
                                )
                            } else {
                                repository.updateProfile(currentProfile.copy(playoffBracketJson = refreshedBracket.toJson()))
                            }
                        }
                    }
                }

                triggerAutoSaveInternal(currentProfileId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun deleteGameLog(log: GameLog, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repository.deleteGameLog(log)
            if (result.isFailure) {
                val errorMsg = result.exceptionOrNull()?.message ?: "Cannot delete game log."
                _errorMessage.value = errorMsg
                onResult?.invoke(false, errorMsg)
            } else {
                triggerAutoSaveInternal()
                onResult?.invoke(true, null)
            }
        }
    }

    fun clearXpPreview() {
        _xpPreview.value = null
    }

    fun buyAttributeUpgrade(attributeId: Int, newValue: Int) {
        viewModelScope.launch {
            val success = repository.upgradeAttribute(attributeId, newValue)
            if (!success) {
                _errorMessage.value = "Cannot afford this upgrade."
            } else {
                triggerAutoSaveInternal()
            }
        }
    }

    fun buyBadgeUpgrade(badgeId: Int, targetTier: String) {
        viewModelScope.launch {
            val success = repository.upgradeBadge(badgeId, targetTier)
            if (!success) {
                _errorMessage.value = "Cannot afford this badge upgrade."
            } else {
                triggerAutoSaveInternal()
            }
        }
    }

    fun markUpgradeAppliedIn2k(historyId: Int) {
        viewModelScope.launch {
            repository.markUpgradeAppliedIn2k(historyId)
            triggerAutoSaveInternal()
        }
    }

    fun markAllUpgradesAppliedIn2k() {
        viewModelScope.launch {
            activeProfileId.value?.let { id ->
                repository.markAllUpgradesAppliedIn2k(id)
                triggerAutoSaveInternal(id)
            }
        }
    }

    fun undoUpgrade(historyId: Int) {
        viewModelScope.launch {
            val success = repository.undoUpgrade(historyId)
            if (!success) {
                _errorMessage.value = "Cannot undo this upgrade."
            } else {
                triggerAutoSaveInternal()
            }
        }
    }

    fun updateManualOvr(newOvr: Int) {
        viewModelScope.launch {
            val profile = activeProfile.value ?: return@launch
            val updated = profile.copy(overallRating = newOvr.coerceIn(60, 99))
            repository.updateProfile(updated)
            triggerAutoSaveInternal(profile.id)
        }
    }

    fun updateQuarterLength(minutes: Int) {
        viewModelScope.launch {
            settingsDataStore.saveQuarterLengthMinutes(minutes)
        }
    }

    fun updateNormalizeQuarterXp(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveNormalizeQuarterXp(enabled)
        }
    }

    fun updateSelectedThemeId(id: String) {
        viewModelScope.launch {
            settingsDataStore.saveSelectedThemeId(id)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.saveThemeMode(mode)
        }
    }

    fun updatePureBlackAmoled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.savePureBlackAmoled(enabled)
        }
    }

    fun updatePersonalizeColors(primary: Color, secondary: Color) {
        viewModelScope.launch {
            val primaryLong = primary.toArgb().toLong() and 0xFFFFFFFFL
            val secondaryLong = secondary.toArgb().toLong() and 0xFFFFFFFFL
            settingsDataStore.savePersonalizeColors(primaryLong, secondaryLong)
        }
    }

    fun claimAward(
        awardName: String,
        category: String,
        seasonNumber: Int,
        xpGranted: Int
    ) {
        viewModelScope.launch {
            val currentProfileId = activeProfileId.value ?: return@launch
            try {
                repository.claimAward(
                    profileId = currentProfileId,
                    awardName = awardName,
                    category = category,
                    seasonNumber = seasonNumber,
                    xpGranted = xpGranted
                )
                triggerAutoSaveInternal(currentProfileId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun deleteAward(award: PlayerAward) {
        viewModelScope.launch {
            try {
                val success = repository.deleteAward(award)
                if (!success) {
                    _errorMessage.value = "Failed to delete award."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun getUnappliedUpgradeCount(): Int {
        return uiState.value.upgradeHistory.count { !it.isAppliedIn2k }
    }

    fun getAttributeCategoryOrder(): List<String> {
        return listOf(
            "Outside Scoring",
            "Inside Scoring",
            "Playmaking",
            "Defense",
            "Rebounding",
            "Athleticism"
        )
    }

    fun getAttributesByCategory(category: String): List<PlayerAttribute> {
        return uiState.value.attributes.filter { it.category == category }
    }

    fun calculateXpPreview(
        points: Int,
        rebounds: Int,
        assists: Int,
        steals: Int,
        blocks: Int,
        turnovers: Int,
        fieldGoalsAttempted: Int,
        fieldGoalsMade: Int,
        isWin: Boolean,
        isPlayoff: Boolean,
        teammateGrade: String,
        quarterMinutes: Int,
        useQuarterNormalizer: Boolean,
        isDoubleDouble: Boolean,
        isTripleDouble: Boolean,
        isPlayerOfTheGame: Boolean,
        isPlayerOfTheWeek: Boolean,
        isAllStar: Boolean,
        isMajorAward: Boolean
    ) {
        val profile = activeProfile.value ?: return
        val inputs = ProgressionCalculator.GameInputs(
            points = points,
            rebounds = rebounds,
            assists = assists,
            steals = steals,
            blocks = blocks,
            turnovers = turnovers,
            fieldGoalsAttempted = fieldGoalsAttempted,
            fieldGoalsMade = fieldGoalsMade,
            isWin = isWin,
            isPlayoff = isPlayoff,
            teammateGrade = teammateGrade,
            quarterMinutes = quarterMinutes,
            useQuarterNormalizer = useQuarterNormalizer,
            isDoubleDouble = isDoubleDouble,
            isTripleDouble = isTripleDouble,
            isPlayerOfTheGame = isPlayerOfTheGame,
            isPlayerOfTheWeek = isPlayerOfTheWeek,
            isAllStar = isAllStar,
            isMajorAward = isMajorAward
        )
        val result = ProgressionCalculator.calculateGameXp(
            inputs,
            profile.xpRemainderForAp,
            profile.xpRemainderForBp
        )
        _xpPreview.value = result
    }

    suspend fun exportActiveCareerToJson(): String? {
        val currentProfileId = activeProfileId.value ?: return null
        return backupManager.exportCareerToJson(currentProfileId)
    }

    suspend fun buildSeasonSummary(profileId: Int, seasonNumber: Int): SeasonSummary {
        val profile = repository.getProfile(profileId) ?: return SeasonSummary()
        val allLogs = repository.getAllGameLogs(profileId)
        val seasonLogs = allLogs.filter { it.seasonNumber == seasonNumber }
        val nonPlayoffLogs = seasonLogs.filter { !it.isPlayoff }

        val wins = nonPlayoffLogs.count { it.isWin }
        val losses = nonPlayoffLogs.size - wins
        val gamesPlayed = nonPlayoffLogs.size

        val totalMinutes = nonPlayoffLogs.sumOf { it.minutesPlayed }
        val mpg = if (gamesPlayed > 0) totalMinutes.toDouble() / gamesPlayed else 0.0

        val ppg = if (gamesPlayed > 0) nonPlayoffLogs.sumOf { it.points }.toDouble() / gamesPlayed else 0.0
        val rpg = if (gamesPlayed > 0) nonPlayoffLogs.sumOf { it.rebounds }.toDouble() / gamesPlayed else 0.0
        val apg = if (gamesPlayed > 0) nonPlayoffLogs.sumOf { it.assists }.toDouble() / gamesPlayed else 0.0
        val spg = if (gamesPlayed > 0) nonPlayoffLogs.sumOf { it.steals }.toDouble() / gamesPlayed else 0.0
        val bpg = if (gamesPlayed > 0) nonPlayoffLogs.sumOf { it.blocks }.toDouble() / gamesPlayed else 0.0

        val totalFgMade = nonPlayoffLogs.sumOf { it.fieldGoalsMade }
        val totalFgAtt = nonPlayoffLogs.sumOf { it.fieldGoalsAttempted }
        val fgPct = if (totalFgAtt > 0) (totalFgMade.toDouble() / totalFgAtt) * 100.0 else 0.0

        val totalFtMade = nonPlayoffLogs.sumOf { it.freeThrowsMade }
        val totalFtAtt = nonPlayoffLogs.sumOf { it.freeThrowsAttempted }
        val ftPct = if (totalFtAtt > 0) (totalFtMade.toDouble() / totalFtAtt) * 100.0 else 0.0

        val doubleDoubles = nonPlayoffLogs.count {
            (it.points >= 10 && it.rebounds >= 10) || (it.points >= 10 && it.assists >= 10) || (it.rebounds >= 10 && it.assists >= 10)
        }
        val tripleDoubles = nonPlayoffLogs.count {
            it.points >= 10 && it.rebounds >= 10 && it.assists >= 10
        }

        val bestGame = nonPlayoffLogs.maxByOrNull { it.points * 2 + it.rebounds + it.assists * 1.5 + it.steals * 2 + it.blocks * 2 }
        val seasonHighSummary = if (bestGame != null) {
            "${bestGame.points} PTS, ${bestGame.rebounds} REB, ${bestGame.assists} AST"
        } else {
            "N/A"
        }

        val allHistory = repository.getAllUpgradeHistory(profileId)
        val seasonHistory = allHistory.filter { it.seasonNumber == seasonNumber }

        val allAttrs = repository.getAllAttributesList(profileId)
        val attrMap = allAttrs.associateBy { it.id }

        val attrUpgrades = seasonHistory.filter { it.entityType == "ATTRIBUTE" }
            .groupBy { it.entityId }
            .mapNotNull { (entityId, rows) ->
                val name = attrMap[entityId]?.attributeName ?: "Attribute #$entityId"
                val minOld = rows.minOf { it.oldValue }
                val maxNew = rows.maxOf { it.newValue }
                val diff = maxNew - minOld
                if (diff > 0) "$name: $minOld ➔ $maxNew (+$diff)" else null
            }.joinToString("; ")

        val tiers = listOf("None", "Bronze", "Silver", "Gold", "HOF", "Legend")
        val allBadges = repository.getAllBadges(profileId)
        val badgeMap = allBadges.associateBy { it.id }

        val badgeUpgrades = seasonHistory.filter { it.entityType == "BADGE" }
            .groupBy { it.entityId }
            .mapNotNull { (entityId, rows) ->
                val name = badgeMap[entityId]?.badgeName ?: "Badge #$entityId"
                val minOldTier = tiers.getOrElse(rows.minOf { it.oldValue }) { "None" }
                val maxNewTier = tiers.getOrElse(rows.maxOf { it.newValue }) { "Legend" }
                "$name: $minOldTier ➔ $maxNewTier"
            }.joinToString("; ")

        val seasonAwards = repository.getAllAwards(profileId).filter { it.seasonNumber == seasonNumber }
        val awardsSummary = if (seasonAwards.isNotEmpty()) {
            seasonAwards.joinToString(", ") { it.awardName }
        } else {
            "None"
        }

        val totalXpEarned = seasonLogs.sumOf { it.xpEarned } + seasonAwards.sumOf { it.xpGranted }

        val existingSummary = repository.getSeasonSummaryForSeason(profileId, seasonNumber)

        return SeasonSummary(
            id = existingSummary?.id ?: 0,
            profileId = profileId,
            seasonNumber = seasonNumber,
            wins = wins,
            losses = losses,
            gamesPlayed = gamesPlayed,
            mpg = mpg,
            totalMinutes = totalMinutes,
            ppg = ppg,
            rpg = rpg,
            apg = apg,
            spg = spg,
            bpg = bpg,
            fgPct = fgPct,
            ftPct = ftPct,
            doubleDoubles = doubleDoubles,
            tripleDoubles = tripleDoubles,
            seasonHighSummary = seasonHighSummary,
            startOvr = profile.seasonStartOvr,
            endOvr = profile.overallRating,
            attributeUpgradesSummary = attrUpgrades.ifEmpty { "None" },
            badgeUpgradesSummary = badgeUpgrades.ifEmpty { "None" },
            awardsSummary = awardsSummary,
            totalXpEarned = totalXpEarned,
            timestamp = System.currentTimeMillis()
        )
    }

    // ── Playoff Series Result Events ──────────────────────────────────────────────
    /** Emitted when a playoff series ends. UI observes this to show dialogs. */
    private val _playoffSeriesEvent = MutableStateFlow<PlayoffSeriesEvent?>(null)
    val playoffSeriesEvent: StateFlow<PlayoffSeriesEvent?> = _playoffSeriesEvent.asStateFlow()

    fun clearPlayoffSeriesEvent() { _playoffSeriesEvent.value = null }

    sealed class PlayoffSeriesEvent {
        data class SeriesWon(val nextRoundName: String, val bracket: PlayoffBracketState) : PlayoffSeriesEvent()
        data class NbaChampion(val bracket: PlayoffBracketState, val playoffGamesWon: Int, val playoffGamesLost: Int) : PlayoffSeriesEvent()
        data class Eliminated(val roundName: String, val seriesWins: Int, val seriesLosses: Int, val bracket: PlayoffBracketState) : PlayoffSeriesEvent()
    }

    /** Called from the Bracket Setup dialog — initializes bracket and enters playoff mode. */
    fun startPlayoffs(bracket: PlayoffBracketState, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val currentProfile = activeProfile.value ?: return@launch
            val updated = currentProfile.copy(
                isInPlayoffs = true,
                playoffBracketJson = bracket.toJson()
            )
            repository.updateProfile(updated)
            triggerAutoSaveInternal(currentProfile.id)
            onComplete?.invoke()
        }
    }

    fun continueToPlayoffs(onComplete: (() -> Unit)? = null) {
        // Opens the bracket setup dialog in UI; just saves season summary here
        viewModelScope.launch {
            val currentProfile = activeProfile.value ?: return@launch
            val summary = buildSeasonSummary(currentProfile.id, currentProfile.currentSeason)
            repository.saveSeasonSummary(summary)
            triggerAutoSaveInternal(currentProfile.id)
            onComplete?.invoke()
        }
    }

    fun advanceToNextSeason(playoffResult: String = "", onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val currentProfile = activeProfile.value ?: return@launch
            val baseSummary = buildSeasonSummary(currentProfile.id, currentProfile.currentSeason)
            val summary = if (playoffResult.isNotBlank()) baseSummary.copy(playoffResult = playoffResult) else baseSummary
            repository.saveSeasonSummary(summary)

            val updated = currentProfile.copy(
                currentSeason = currentProfile.currentSeason + 1,
                seasonStartOvr = currentProfile.overallRating,
                isInPlayoffs = false,
                playoffBracketJson = null
            )
            repository.updateProfile(updated)

            triggerAutoSaveInternal(currentProfile.id)
            onComplete?.invoke()
        }
    }

    /** Updates a CPU series in the bracket (tap-to-set-winner from bracket sheet). */
    fun setCpuSeriesWinner(seriesId: String, winnerAbbr: String) {
        viewModelScope.launch {
            val currentProfile = activeProfile.value ?: return@launch
            val bracket = PlayoffBracketState.fromJson(currentProfile.playoffBracketJson) ?: return@launch
            val updatedBracket = advanceCpuSeriesWinner(bracket, seriesId, winnerAbbr)
            repository.updateProfile(currentProfile.copy(playoffBracketJson = updatedBracket.toJson()))
            triggerAutoSaveInternal(currentProfile.id)
        }
    }

    /** Sim/complete all remaining unfinished CPU series in the current round in one tap. */
    fun completeRemainingCpuSeries() {
        viewModelScope.launch {
            val currentProfile = activeProfile.value ?: return@launch
            val bracket = PlayoffBracketState.fromJson(currentProfile.playoffBracketJson) ?: return@launch
            val completed = completeCpuSeriesInRound(bracket)
            val refreshed = PlayoffBracketState.refreshBracket(completed)
            repository.updateProfile(currentProfile.copy(playoffBracketJson = refreshed.toJson()))
            triggerAutoSaveInternal(currentProfile.id)
        }
    }

    private fun completeCpuSeriesInRound(bracket: PlayoffBracketState): PlayoffBracketState {
        fun finishCpu(s: PlayoffSeries): PlayoffSeries {
            if (s.isUserSeries || s.isFinished) return s
            // Sim: top-seeded team wins the series 4-0
            return s.copy(topWins = 4, isFinished = true)
        }
        return when (bracket.currentRound) {
            1 -> bracket.copy(round1Matchups = bracket.round1Matchups.map(::finishCpu))
            2 -> bracket.copy(semisMatchups = bracket.semisMatchups.map(::finishCpu))
            3 -> bracket.copy(confFinalsMatchup = bracket.confFinalsMatchup?.let(::finishCpu))
            4 -> bracket.copy(nbaFinalsMatchup = bracket.nbaFinalsMatchup?.let(::finishCpu))
            else -> bracket
        }
    }

    /** Logs a playoff game, updates the user's series score, and fires a PlayoffSeriesEvent if the series ends. */
    fun logPlayoffGame(
        points: Int, rebounds: Int, assists: Int, steals: Int, blocks: Int,
        turnovers: Int, fieldGoalsAttempted: Int, fieldGoalsMade: Int,
        freeThrowsAttempted: Int = 0, freeThrowsMade: Int = 0,
        isWin: Boolean, teammateGrade: String, quarterMinutes: Int,
        useQuarterNormalizer: Boolean, isDoubleDouble: Boolean, isTripleDouble: Boolean
    ) {
        viewModelScope.launch {
            val currentProfileId = activeProfileId.value ?: return@launch
            val currentProfile = activeProfile.value ?: return@launch
            val bracket = PlayoffBracketState.fromJson(currentProfile.playoffBracketJson) ?: return@launch

            // Record the game (always playoff = true for 1.2x XP)
            try {
                val xpResult = repository.recordGameAndCalculateProgression(
                    profileId = currentProfileId,
                    points = points, rebounds = rebounds, assists = assists,
                    steals = steals, blocks = blocks, turnovers = turnovers,
                    fieldGoalsAttempted = fieldGoalsAttempted, fieldGoalsMade = fieldGoalsMade,
                    freeThrowsAttempted = freeThrowsAttempted, freeThrowsMade = freeThrowsMade,
                    isWin = isWin, isPlayoff = true, teammateGrade = teammateGrade,
                    quarterMinutes = quarterMinutes, useQuarterNormalizer = useQuarterNormalizer,
                    isDoubleDouble = isDoubleDouble, isTripleDouble = isTripleDouble,
                    isPlayerOfTheGame = false, isPlayerOfTheWeek = false, isAllStar = false, isMajorAward = false
                )
                _xpPreview.value = xpResult
            } catch (e: Exception) {
                _errorMessage.value = e.message
                return@launch
            }

            // Determine if user is top or bottom team in their active series
            val userSeries = bracket.activeUserSeries ?: run {
                triggerAutoSaveInternal(currentProfileId)
                return@launch
            }
            val userTeamAbbr = NbaTeamCatalog.getScheme(currentProfile.teamName).abbreviation
            val userIsTop = userSeries.topTeamAbbr == userTeamAbbr || userSeries.topSeed == bracket.userSeed

            val updatedSeries = if (userIsTop) {
                if (isWin) userSeries.copy(topWins = userSeries.topWins + 1)
                else userSeries.copy(bottomWins = userSeries.bottomWins + 1)
            } else {
                if (isWin) userSeries.copy(bottomWins = userSeries.bottomWins + 1)
                else userSeries.copy(topWins = userSeries.topWins + 1)
            }

            val userWins = if (userIsTop) updatedSeries.topWins else updatedSeries.bottomWins
            val oppWins  = if (userIsTop) updatedSeries.bottomWins else updatedSeries.topWins
            val updatedBracket = replaceSeries(bracket, updatedSeries)

            when {
                userWins >= 4 -> {
                    val finishedBracket = markSeriesFinished(updatedBracket, updatedSeries.id)
                    if (bracket.currentRound == 4) {
                        // NBA CHAMPIONS
                        val playoffLogs = repository.getAllGameLogs(currentProfileId)
                            .filter { it.seasonNumber == currentProfile.currentSeason && it.isPlayoff }
                        val champWins = playoffLogs.count { it.isWin }
                        val champLosses = playoffLogs.size - champWins
                        repository.claimAward(
                            profileId = currentProfileId,
                            awardName = "NBA Champion",
                            category = "MAJOR",
                            seasonNumber = currentProfile.currentSeason,
                            xpGranted = 500
                        )
                        val finalBracket = finishedBracket.copy(
                            nbaFinalsMatchup = finishedBracket.nbaFinalsMatchup?.copy(isFinished = true)
                        )
                        repository.updateProfile(currentProfile.copy(playoffBracketJson = finalBracket.toJson()))
                        _playoffSeriesEvent.value = PlayoffSeriesEvent.NbaChampion(finalBracket, champWins, champLosses)
                    } else {
                        val advancedBracket = advanceUserToNextRound(finishedBracket, bracket.currentRound)
                        repository.updateProfile(currentProfile.copy(playoffBracketJson = advancedBracket.toJson()))
                        _playoffSeriesEvent.value = PlayoffSeriesEvent.SeriesWon(advancedBracket.currentRoundName, advancedBracket)
                    }
                }
                oppWins >= 4 -> {
                    // ELIMINATED
                    val finishedBracket = markSeriesFinished(updatedBracket, updatedSeries.id)
                    repository.updateProfile(currentProfile.copy(
                        playoffBracketJson = finishedBracket.toJson(),
                        isInPlayoffs = false
                    ))
                    _playoffSeriesEvent.value = PlayoffSeriesEvent.Eliminated(
                        bracket.currentRoundName, userWins, oppWins, finishedBracket
                    )
                }
                else -> {
                    // Series ongoing
                    repository.updateProfile(currentProfile.copy(playoffBracketJson = updatedBracket.toJson()))
                }
            }

            triggerAutoSaveInternal(currentProfileId)
        }
    }

    // ── Private bracket helpers ───────────────────────────────────────────────

    private fun replaceSeries(bracket: PlayoffBracketState, series: PlayoffSeries): PlayoffBracketState {
        return when (series.round) {
            1 -> bracket.copy(round1Matchups = bracket.round1Matchups.map { if (it.id == series.id) series else it })
            2 -> bracket.copy(semisMatchups = bracket.semisMatchups.map { if (it.id == series.id) series else it })
            3 -> bracket.copy(confFinalsMatchup = if (bracket.confFinalsMatchup?.id == series.id) series else bracket.confFinalsMatchup)
            4 -> bracket.copy(nbaFinalsMatchup = if (bracket.nbaFinalsMatchup?.id == series.id) series else bracket.nbaFinalsMatchup)
            else -> bracket
        }
    }

    private fun markSeriesFinished(bracket: PlayoffBracketState, seriesId: String): PlayoffBracketState {
        fun fin(s: PlayoffSeries) = if (s.id == seriesId) s.copy(isFinished = true) else s
        return bracket.copy(
            round1Matchups = bracket.round1Matchups.map(::fin),
            semisMatchups = bracket.semisMatchups.map(::fin),
            confFinalsMatchup = bracket.confFinalsMatchup?.let(::fin),
            nbaFinalsMatchup = bracket.nbaFinalsMatchup?.let(::fin)
        )
    }

    private fun advanceUserToNextRound(bracket: PlayoffBracketState, completedRound: Int): PlayoffBracketState {
        val nextRound = completedRound + 1
        val userSeed = bracket.userSeed

        // Do NOT auto-sweep CPU series to 4-0. Only mark a series finished if a team already reached 4 wins.
        fun autoFinishCpu(s: PlayoffSeries): PlayoffSeries {
            if (s.isUserSeries || s.isFinished) return s
            return if (s.topWins >= 4 || s.bottomWins >= 4) s.copy(isFinished = true) else s
        }

        val finR1    = if (completedRound == 1) bracket.round1Matchups.map(::autoFinishCpu) else bracket.round1Matchups
        val finSemis = if (completedRound == 2) bracket.semisMatchups.map(::autoFinishCpu)  else bracket.semisMatchups
        val finCF    = if (completedRound == 3) bracket.confFinalsMatchup?.let(::autoFinishCpu) else bracket.confFinalsMatchup

        val base = bracket.copy(currentRound = nextRound, round1Matchups = finR1, semisMatchups = finSemis, confFinalsMatchup = finCF)

        return when (nextRound) {
            2 -> {
                val w = finR1.map { Pair(it.winnerAbbr ?: it.topTeamAbbr, it.winnerSeed ?: it.topSeed) }
                val (a0, s0) = w.getOrElse(0) { Pair("", 1) }
                val (a1, s1) = w.getOrElse(1) { Pair("", 4) }
                val (a2, s2) = w.getOrElse(2) { Pair("", 3) }
                val (a3, s3) = w.getOrElse(3) { Pair("", 2) }
                base.copy(semisMatchups = listOf(
                    PlayoffSeries("semi_1", 2, a0, s0, 0, a1, s1, 0, s0 == userSeed || s1 == userSeed),
                    PlayoffSeries("semi_2", 2, a2, s2, 0, a3, s3, 0, s2 == userSeed || s3 == userSeed)
                ))
            }
            3 -> {
                val w = finSemis.map { Pair(it.winnerAbbr ?: it.topTeamAbbr, it.winnerSeed ?: it.topSeed) }
                val (a0, s0) = w.getOrElse(0) { Pair("", 1) }
                val (a1, s1) = w.getOrElse(1) { Pair("", 2) }
                base.copy(confFinalsMatchup = PlayoffSeries("conf_finals", 3, a0, s0, 0, a1, s1, 0, s0 == userSeed || s1 == userSeed))
            }
            4 -> {
                val confChamp  = finCF?.winnerAbbr  ?: bracket.userTeamAbbr
                val confChampSeed = finCF?.winnerSeed ?: userSeed
                val oppAbbr = base.finalsOpponentAbbr
                val isUserTop = confChampSeed == 1
                base.copy(nbaFinalsMatchup = PlayoffSeries(
                    "nba_finals", 4,
                    topTeamAbbr   = if (isUserTop) confChamp else oppAbbr,
                    topSeed       = if (isUserTop) confChampSeed else 1,
                    topWins       = 0,
                    bottomTeamAbbr = if (isUserTop) oppAbbr else confChamp,
                    bottomSeed    = if (isUserTop) 1 else confChampSeed,
                    bottomWins    = 0,
                    isUserSeries  = true
                ))
            }
            else -> base
        }
    }

    private fun advanceCpuSeriesWinner(bracket: PlayoffBracketState, seriesId: String, winnerAbbr: String): PlayoffBracketState {
        fun update(s: PlayoffSeries): PlayoffSeries {
            if (s.id != seriesId) return s
            return if (s.topTeamAbbr == winnerAbbr) s.copy(topWins = 4, isFinished = true)
            else s.copy(bottomWins = 4, isFinished = true)
        }
        return bracket.copy(
            round1Matchups = bracket.round1Matchups.map(::update),
            semisMatchups = bracket.semisMatchups.map(::update),
            confFinalsMatchup = bracket.confFinalsMatchup?.let(::update),
            nbaFinalsMatchup = bracket.nbaFinalsMatchup?.let(::update)
        )
    }

    fun importCareerFromJson(
        jsonString: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = backupManager.importCareerFromJson(jsonString)
            result.onSuccess { newProfileId ->
                selectProfile(newProfileId)
                onSuccess()
            }.onFailure { e ->
                onError(e.localizedMessage ?: "Invalid career save format.")
            }
        }
    }

    fun isCareerSaveJson(jsonString: String): Boolean {
        return backupManager.isCareerSaveJson(jsonString)
    }

    fun parseCustomPlayerTemplate(jsonString: String): CustomPlayerTemplate {
        return backupManager.parseCustomPlayerTemplateJson(jsonString)
    }

    fun exportCustomPlayerTemplateToJson(template: CustomPlayerTemplate): String {
        return backupManager.exportCustomPlayerTemplateToJson(template)
    }

    suspend fun parsePlayerSetupFromUri(context: android.content.Context, uri: android.net.Uri): Result<CustomPlayerTemplate> {
        return backupManager.parsePlayerSetupFromUri(context, uri)
    }

    fun parsePlayerSetupFromJson(jsonString: String): Result<CustomPlayerTemplate> {
        return backupManager.parsePlayerSetupFromJson(jsonString)
    }
}
