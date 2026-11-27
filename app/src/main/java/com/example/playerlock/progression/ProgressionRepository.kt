package com.example.playerlock.progression

import com.example.playerlock.data.AppDatabase
import com.example.playerlock.data.AttributeSeedData
import com.example.playerlock.data.BadgeSeedData
import com.example.playerlock.data.GameLog
import com.example.playerlock.data.PlayerAttribute
import com.example.playerlock.data.PlayerBadge
import com.example.playerlock.data.PlayerProfile
import com.example.playerlock.data.SeasonSummary
import com.example.playerlock.data.UpgradeHistory
import kotlinx.coroutines.flow.Flow

class ProgressionRepository(private val database: AppDatabase) {

    // -----------------------------------------------------------------------
    // Profile
    // -----------------------------------------------------------------------

    fun getAllProfiles(): Flow<List<PlayerProfile>> {
        return database.playerProfileDao().getAllProfiles()
    }

    suspend fun getProfile(profileId: Int): PlayerProfile? {
        return database.playerProfileDao().getProfileById(profileId)
    }

    suspend fun getProfile(): PlayerProfile? {
        return database.playerProfileDao().getDefaultProfile()
    }

    fun getProfileFlow(profileId: Int): Flow<PlayerProfile?> {
        return database.playerProfileDao().getProfileFlowById(profileId)
    }

    fun getProfileFlow(): Flow<PlayerProfile> {
        return database.playerProfileDao().getDefaultProfileFlow()
    }

    suspend fun updateProfile(profile: PlayerProfile) {
        database.playerProfileDao().updateProfile(profile)
    }

    suspend fun deleteProfile(profileId: Int) {
        database.playerProfileDao().deleteProfileById(profileId)
    }

    suspend fun createPlayer(
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
    ): Int {
        val archetypeAttributes = AttributeSeedData.generateAttributesForArchetype(
            archetype = archetype,
            position = position,
            startingOvr = startingOvr
        )

        // Seed all 36 MyNBA attributes specifically for this new profileId
        val attributes = AttributeSeedData.ATTRIBUTES.map { attr ->
            val valForAttr = customAttributes?.get(attr.attributeName)
                ?: archetypeAttributes[attr.attributeName]
                ?: startingOvr
            attr.copy(id = 0, profileId = 0, currentValue = valForAttr)
        }

        // Auto-calculate position-weighted OVR from attributes
        val calculatedOvr = ProgressionCalculator.calculateOverallRating(position, attributes)

        val profile = PlayerProfile(
            id = 0,
            playerName = playerName,
            position = position,
            overallRating = calculatedOvr,
            avatarPath = avatarPath,
            heightInches = heightInches,
            weightLbs = weightLbs,
            jerseyNumber = jerseyNumber,
            teamName = teamName,
            createdAt = System.currentTimeMillis()
        )
        val newProfileId = database.playerProfileDao().insertProfile(profile).toInt()

        val attributesWithProfileId = attributes.map { it.copy(profileId = newProfileId) }
        database.playerAttributeDao().insertAttributes(attributesWithProfileId)

        // Seed all badges specifically for this new profileId
        val badges = BadgeSeedData.BADGES.map { badge ->
            val tierForBadge = customBadges?.get(badge.badgeName) ?: "None"
            badge.copy(id = 0, profileId = newProfileId, tier = tierForBadge)
        }
        database.playerBadgeDao().insertBadges(badges)

        return newProfileId
    }

    // -----------------------------------------------------------------------
    // Attributes
    // -----------------------------------------------------------------------

    fun getAllAttributes(profileId: Int): Flow<List<PlayerAttribute>> {
        return database.playerAttributeDao().getAllAttributes(profileId)
    }

    suspend fun getAllAttributesList(profileId: Int): List<PlayerAttribute> {
        syncExistingAttributes(profileId)
        return database.playerAttributeDao().getAllAttributesList(profileId)
    }

    // -----------------------------------------------------------------------
    // Safe sync for existing profiles (legacy migration)
    // -----------------------------------------------------------------------

    /**
     * Synchronizes an existing profile's attribute rows to the new 5-category, 36-attribute
     * catalog without wiping purchased upgrades:
     *   - removes any legacy "Overall Durability" row,
     *   - inserts "Intangibles" and "Potential" if missing,
     *   - updates `category` on existing rows to match the new category map.
     */
    suspend fun syncExistingAttributes(profileId: Int) {
        val existing = database.playerAttributeDao().getAllAttributesList(profileId)
        val existingByName = existing.associate { it.attributeName to it }

        // Remove legacy "Overall Durability" row (preserves upgrades on all other attributes)
        val durability = existingByName["Overall Durability"]
        if (durability != null) {
            database.playerAttributeDao().deleteOverallDurability(profileId)
        }

        // Build the desired category & sortOrder map from the seed catalog
        val desired = AttributeSeedData.ATTRIBUTES.associate { it.attributeName to it }

        // Update category and sortOrder on existing rows that changed
        val toUpdate = existing.filter { attr ->
            attr.attributeName != "Overall Durability" &&
                    desired.containsKey(attr.attributeName) &&
                    (desired[attr.attributeName]!!.category != attr.category ||
                            desired[attr.attributeName]!!.sortOrder != attr.sortOrder)
        }
        if (toUpdate.isNotEmpty()) {
            database.playerAttributeDao().updateAttributes(
                toUpdate.map { attr ->
                    val d = desired[attr.attributeName]!!
                    attr.copy(category = d.category, sortOrder = d.sortOrder)
                }
            )
        }

        // Insert "Intangibles" and "Potential" if missing (starting at 60, preserving nothing since new)
        val missing = AttributeSeedData.ATTRIBUTES.filter { attr ->
            attr.attributeName in listOf("Intangibles", "Potential") && !existingByName.containsKey(attr.attributeName)
        }
        if (missing.isNotEmpty()) {
            database.playerAttributeDao().insertAttributes(
                missing.map { attr ->
                    attr.copy(id = 0, profileId = profileId, currentValue = 60)
                }
            )
        }
    }

    fun getAllAttributes(): Flow<List<PlayerAttribute>> {
        return database.playerAttributeDao().getAllAttributes(1)
    }

    suspend fun getAttributeById(id: Int): PlayerAttribute? {
        return database.playerAttributeDao().getAttributeById(id)
    }

    // -----------------------------------------------------------------------
    // Badges
    // -----------------------------------------------------------------------

    fun getAllBadgesFlow(profileId: Int): Flow<List<PlayerBadge>> {
        return database.playerBadgeDao().getAllBadgesFlow(profileId)
    }

    suspend fun getAllBadges(profileId: Int): List<PlayerBadge> {
        syncExistingBadges(profileId)
        return database.playerBadgeDao().getAllBadges(profileId)
    }

    suspend fun getAllBadges(): List<PlayerBadge> {
        return database.playerBadgeDao().getAllBadges(1)
    }

    suspend fun getBadgeById(id: Int): PlayerBadge? {
        return database.playerBadgeDao().getBadgeById(id)
    }

    /**
     * Synchronises an existing profile's badge rows against the canonical 53-badge catalog:
     *  1. Applies legacy renames (Boxout Beast→Boxout Boss, etc.) preserving tier progress.
     *  2. Deletes any non-custom badge whose name is not in the 53-badge list.
     *  3. Inserts any missing canonical badges at tier 0 (None).
     *  4. Updates `category` and `sortOrder` on every row to match the catalog.
     */
    suspend fun syncExistingBadges(profileId: Int) {
        val dao = database.playerBadgeDao()
        val canonicalNames = BadgeSeedData.BADGE_META.keys

        // Step 1: Apply legacy renames
        for ((oldName, newName) in BadgeSeedData.LEGACY_RENAMES) {
            dao.renameBadge(profileId, oldName, newName)
        }

        // Reload after renames
        val existing = dao.getAllBadges(profileId)
        val existingByName = existing.associate { it.badgeName to it }

        // Step 2: Delete non-custom badges not in the 53-badge list.
        // "Custom" badges are those whose category is not one of the 6 standard categories
        // AND whose name doesn't appear in the canonical set.
        val standardCategories = setOf("Finishing", "Shooting", "Playmaking", "Defense", "Physicals", "Rebounding")
        val toDelete = existing.filter { badge ->
            badge.badgeName !in canonicalNames &&
            badge.category in standardCategories  // only delete catalog-category rows, leave true custom
        }
        for (badge in toDelete) {
            dao.deleteBadgeByName(profileId, badge.badgeName)
        }

        // Step 3: Insert missing canonical badges at tier None
        val missingBadges = BadgeSeedData.BADGES.filter { seed ->
            !existingByName.containsKey(seed.badgeName)
        }
        if (missingBadges.isNotEmpty()) {
            dao.insertBadges(missingBadges.map { it.copy(id = 0, profileId = profileId, tier = "None") })
        }

        // Step 4: Update category and sortOrder on all canonical rows
        for ((name, meta) in BadgeSeedData.BADGE_META) {
            val (category, sortOrder) = meta
            dao.updateCategoryAndSortOrder(profileId, name, category, sortOrder)
        }
    }

    // -----------------------------------------------------------------------
    // Game log + progression
    // -----------------------------------------------------------------------

    suspend fun recordGameAndCalculateProgression(
        profileId: Int,
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
    ): ProgressionCalculator.GameXpResult {

        val profile = database.playerProfileDao().getProfileById(profileId)
            ?: error("No player profile found for id $profileId. Seed the database first.")

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

        val xpResult = ProgressionCalculator.calculateGameXp(
            inputs,
            profile.xpRemainderForAp,
            profile.xpRemainderForBp
        )

        val updatedProfile = profile.copy(
            availableAp = profile.availableAp + xpResult.apGained,
            availableBp = profile.availableBp + xpResult.bpGained,
            xpRemainderForAp = xpResult.xpRemainderForAp,
            xpRemainderForBp = xpResult.xpRemainderForBp,
            weightAllowanceLbs = profile.weightAllowanceLbs + 2
        )
        database.playerProfileDao().updateProfile(updatedProfile)

        val gameLog = GameLog(
            profileId = profileId,
            seasonNumber = profile.currentSeason,
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
            playerOfTheGame = isPlayerOfTheGame,
            playerOfTheWeek = isPlayerOfTheWeek,
            allStar = isAllStar,
            majorAward = isMajorAward
        )
        database.gameLogDao().insertGameLog(gameLog)

        return xpResult
    }

    suspend fun recordGameAndCalculateProgression(
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
    ): ProgressionCalculator.GameXpResult {
        val defaultProfile = database.playerProfileDao().getDefaultProfile()
        val profileId = defaultProfile?.id ?: 1
        return recordGameAndCalculateProgression(
            profileId = profileId,
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
    }

    // -----------------------------------------------------------------------
    // Upgrades
    // -----------------------------------------------------------------------

    suspend fun upgradeAttribute(
        attributeId: Int,
        newValue: Int
    ): Boolean {
        val attribute = database.playerAttributeDao().getAttributeById(attributeId)
            ?: return false
        val profile = database.playerProfileDao().getProfileById(attribute.profileId)
            ?: return false
        val result = ProgressionCalculator.applyAttributeUpgrade(attribute, profile, newValue)
            ?: return false
        val (updatedAttribute, rawHistory) = result
        val history = rawHistory.copy(seasonNumber = profile.currentSeason)
        database.playerAttributeDao().updateAttribute(updatedAttribute)
        database.upgradeHistoryDao().insertUpgradeHistory(history)

        // Auto-recalculate and save overall rating
        val allAttrs = database.playerAttributeDao().getAllAttributesList(profile.id)
        val newOvr = ProgressionCalculator.calculateOverallRating(profile.position, allAttrs)

        val updatedProfile = when (history.currencyType) {
            "AP" -> profile.copy(availableAp = profile.availableAp - history.costSpent, overallRating = newOvr)
            "BP" -> profile.copy(availableBp = profile.availableBp - history.costSpent, overallRating = newOvr)
            else -> profile.copy(overallRating = newOvr)
        }
        database.playerProfileDao().updateProfile(updatedProfile)
        return true
    }

    suspend fun upgradeBadge(
        badgeId: Int,
        targetTier: String
    ): Boolean {
        val badge = database.playerBadgeDao().getBadgeById(badgeId)
            ?: return false
        val profile = database.playerProfileDao().getProfileById(badge.profileId)
            ?: return false
        val result = ProgressionCalculator.applyBadgeUpgrade(badge, profile, targetTier)
            ?: return false
        val (updatedBadge, rawHistory) = result
        val history = rawHistory.copy(seasonNumber = profile.currentSeason)
        val updatedProfile = when (history.currencyType) {
            "AP" -> profile.copy(availableAp = profile.availableAp - history.costSpent)
            "BP" -> profile.copy(availableBp = profile.availableBp - history.costSpent)
            else -> profile
        }
        database.playerProfileDao().updateProfile(updatedProfile)
        database.playerBadgeDao().updateBadge(updatedBadge)
        database.upgradeHistoryDao().insertUpgradeHistory(history)
        return true
    }

    suspend fun markUpgradeAppliedIn2k(historyId: Int): Boolean {
        return database.upgradeHistoryDao().markUpgradeAppliedIn2k(historyId) > 0
    }

    suspend fun markAllUpgradesAppliedIn2k(profileId: Int) {
        database.upgradeHistoryDao().markAllAppliedIn2k(profileId)
    }

    suspend fun markAllUpgradesAppliedIn2k() {
        val defaultProfile = database.playerProfileDao().getDefaultProfile()
        val profileId = defaultProfile?.id ?: 1
        database.upgradeHistoryDao().markAllAppliedIn2k(profileId)
    }

    // -----------------------------------------------------------------------
    // Undo
    // -----------------------------------------------------------------------

    fun getUnappliedHistoryFlow(profileId: Int): Flow<List<UpgradeHistory>> {
        return database.upgradeHistoryDao().getUnappliedHistoryFlow(profileId)
    }

    fun getAllUpgradeHistoryFlow(profileId: Int): Flow<List<UpgradeHistory>> {
        return database.upgradeHistoryDao().getAllUpgradeHistoryFlow(profileId)
    }

    suspend fun getUndoableHistory(profileId: Int): List<UpgradeHistory> {
        return database.upgradeHistoryDao().getUnappliedHistory(profileId)
    }

    suspend fun getUndoableHistory(): List<UpgradeHistory> {
        val defaultProfile = database.playerProfileDao().getDefaultProfile()
        val profileId = defaultProfile?.id ?: 1
        return database.upgradeHistoryDao().getUnappliedHistory(profileId)
    }

    suspend fun undoUpgrade(historyId: Int): Boolean {
        val history = database.upgradeHistoryDao().getHistoryById(historyId)
            ?: return false
        if (history.isAppliedIn2k) return false
        val targetProfile = database.playerProfileDao().getProfileById(history.profileId)
            ?: return false
        val attribute = when (history.entityType) {
            "ATTRIBUTE" -> database.playerAttributeDao().getAttributeById(history.entityId)
            else -> null
        }
        val badge = when (history.entityType) {
            "BADGE" -> database.playerBadgeDao().getBadgeById(history.entityId)
            else -> null
        }
        val undoResult = ProgressionCalculator.undoUpgrade(history, targetProfile, attribute, badge)
        if (!undoResult.success) return false
        val updatedProfile = targetProfile.copy(
            availableAp = targetProfile.availableAp + undoResult.refundedAp,
            availableBp = targetProfile.availableBp + undoResult.refundedBp
        )
        when (history.entityType) {
            "WEIGHT" -> {
                database.playerProfileDao().updateProfile(updatedProfile.copy(weightLbs = history.oldValue))
            }
            "ATTRIBUTE" -> {
                attribute?.let { attr ->
                    database.playerAttributeDao().updateAttribute(attr.copy(currentValue = history.oldValue))
                    val allAttrs = database.playerAttributeDao().getAllAttributesList(targetProfile.id)
                    val newOvr = ProgressionCalculator.calculateOverallRating(targetProfile.position, allAttrs)
                    database.playerProfileDao().updateProfile(updatedProfile.copy(overallRating = newOvr))
                }
            }
            "BADGE" -> {
                badge?.let { b ->
                    val oldTier = when (history.oldValue) {
                        0 -> "None"
                        1 -> "Bronze"
                        2 -> "Silver"
                        3 -> "Gold"
                        4 -> "HOF"
                        5 -> "Legend"
                        else -> "None"
                    }
                    database.playerBadgeDao().updateBadge(b.copy(tier = oldTier))
                    database.playerProfileDao().updateProfile(updatedProfile)
                }
            }
            else -> {
                database.playerProfileDao().updateProfile(updatedProfile)
            }
        }
        database.upgradeHistoryDao().deleteUpgradeHistory(history)
        return true
    }

    // -----------------------------------------------------------------------
    // Game logs
    // -----------------------------------------------------------------------

    fun getAllGameLogsFlow(profileId: Int): Flow<List<GameLog>> {
        return database.gameLogDao().getAllGameLogsFlow(profileId)
    }

    suspend fun getAllGameLogs(profileId: Int): List<GameLog> {
        return database.gameLogDao().getAllGameLogs(profileId)
    }

    suspend fun getAllGameLogs(): List<GameLog> {
        val defaultProfile = database.playerProfileDao().getDefaultProfile()
        val profileId = defaultProfile?.id ?: 1
        return database.gameLogDao().getAllGameLogs(profileId)
    }

    suspend fun deleteGameLog(log: GameLog): Result<Unit> {
        val profile = database.playerProfileDao().getProfileById(log.profileId)
            ?: return Result.failure(IllegalStateException("Profile not found"))

        if (log.seasonNumber != profile.currentSeason) {
            return Result.failure(
                IllegalStateException("Cannot delete games from concluded seasons.")
            )
        }

        val xpEarned = log.xpEarned
        val currentApXp = (profile.availableAp * 25) + profile.xpRemainderForAp
        val currentBpXp = (profile.availableBp * 100) + profile.xpRemainderForBp

        if (currentApXp < xpEarned || currentBpXp < xpEarned) {
            return Result.failure(
                IllegalStateException("Cannot delete: you already spent part of this game's $xpEarned XP. Undo unapplied upgrades first.")
            )
        }

        val newApXp = currentApXp - xpEarned
        val newBpXp = currentBpXp - xpEarned

        val updatedProfile = profile.copy(
            availableAp = (newApXp / 25).toInt(),
            xpRemainderForAp = newApXp % 25,
            availableBp = (newBpXp / 100).toInt(),
            xpRemainderForBp = newBpXp % 100
        )

        database.playerProfileDao().updateProfile(updatedProfile)
        database.gameLogDao().deleteGameLog(log)
        return Result.success(Unit)
    }

    suspend fun recordWeightUpgrade(
        profileId: Int,
        seasonNumber: Int,
        oldWeight: Int,
        newWeight: Int,
        costSpent: Int
    ) {
        val history = UpgradeHistory(
            profileId = profileId,
            seasonNumber = seasonNumber,
            entityType = "WEIGHT",
            entityId = 0,
            oldValue = oldWeight,
            newValue = newWeight,
            costSpent = costSpent,
            currencyType = "AP",
            isAppliedIn2k = true,
            timestamp = System.currentTimeMillis()
        )
        database.upgradeHistoryDao().insertUpgradeHistory(history)
    }

    suspend fun getAllUpgradeHistory(profileId: Int): List<UpgradeHistory> {
        return database.upgradeHistoryDao().getAllUpgradeHistory(profileId)
    }

    // -----------------------------------------------------------------------
    // Player Awards & Trophy Case
    // -----------------------------------------------------------------------

    fun getAllAwardsFlow(profileId: Int): Flow<List<com.example.playerlock.data.PlayerAward>> {
        return database.playerAwardDao().getAllAwardsFlow(profileId)
    }

    suspend fun getAllAwards(profileId: Int): List<com.example.playerlock.data.PlayerAward> {
        return database.playerAwardDao().getAllAwards(profileId)
    }

    // -----------------------------------------------------------------------
    // Season Summaries & Recaps Archive
    // -----------------------------------------------------------------------

    fun getAllSeasonSummariesFlow(profileId: Int): Flow<List<SeasonSummary>> {
        return database.seasonSummaryDao().getSeasonSummariesFlow(profileId)
    }

    suspend fun getAllSeasonSummaries(profileId: Int): List<SeasonSummary> {
        return database.seasonSummaryDao().getSeasonSummaries(profileId)
    }

    suspend fun getSeasonSummaryForSeason(profileId: Int, seasonNumber: Int): SeasonSummary? {
        return database.seasonSummaryDao().getSeasonSummaryForSeason(profileId, seasonNumber)
    }

    suspend fun saveSeasonSummary(summary: SeasonSummary): Long {
        return database.seasonSummaryDao().insertSeasonSummary(summary)
    }

    suspend fun claimAward(
        profileId: Int,
        awardName: String,
        category: String,
        seasonNumber: Int,
        xpGranted: Int
    ): com.example.playerlock.data.PlayerAward {
        val profile = database.playerProfileDao().getProfileById(profileId)
            ?: error("Profile not found: $profileId")

        val award = com.example.playerlock.data.PlayerAward(
            id = 0,
            profileId = profileId,
            awardName = awardName,
            category = category,
            seasonNumber = seasonNumber,
            xpGranted = xpGranted,
            timestamp = System.currentTimeMillis()
        )
        val insertedId = database.playerAwardDao().insertAward(award).toInt()

        // Immediately award XP and convert into AP & BP with rollover
        val xpPoolAp = xpGranted + profile.xpRemainderForAp
        val xpPoolBp = xpGranted + profile.xpRemainderForBp

        val apGained = (xpPoolAp / 25).toInt()
        val remainderAp = xpPoolAp % 25

        val bpGained = (xpPoolBp / 100).toInt()
        val remainderBp = xpPoolBp % 100

        val updatedProfile = profile.copy(
            availableAp = profile.availableAp + apGained,
            availableBp = profile.availableBp + bpGained,
            xpRemainderForAp = remainderAp,
            xpRemainderForBp = remainderBp
        )
        database.playerProfileDao().updateProfile(updatedProfile)

        return award.copy(id = insertedId)
    }

    suspend fun deleteAward(award: com.example.playerlock.data.PlayerAward): Boolean {
        val profile = database.playerProfileDao().getProfileById(award.profileId)
            ?: return false

        if (award.seasonNumber != profile.currentSeason) {
            return false
        }

        // Reverse the granted XP from profile
        val currentTotalXpAp = (profile.availableAp * 25.0) + profile.xpRemainderForAp
        val currentTotalXpBp = (profile.availableBp * 100.0) + profile.xpRemainderForBp

        val newTotalXpAp = (currentTotalXpAp - award.xpGranted).coerceAtLeast(0.0)
        val newTotalXpBp = (currentTotalXpBp - award.xpGranted).coerceAtLeast(0.0)

        val updatedProfile = profile.copy(
            availableAp = (newTotalXpAp / 25).toInt(),
            xpRemainderForAp = newTotalXpAp % 25,
            availableBp = (newTotalXpBp / 100).toInt(),
            xpRemainderForBp = newTotalXpBp % 100
        )
        database.playerProfileDao().updateProfile(updatedProfile)
        database.playerAwardDao().deleteAward(award)
        return true
    }

    suspend fun importCareerFromJson(jsonString: String): Int {
        val rootJson = org.json.JSONObject(jsonString)
        val profileJson = rootJson.getJSONObject("profile")

        val importedProfile = PlayerProfile(
            id = 0,
            playerName = profileJson.optString("playerName", "Imported Player"),
            position = profileJson.optString("position", "PG"),
            overallRating = profileJson.optInt("overallRating", 60),
            availableAp = profileJson.optInt("availableAp", 0),
            availableBp = profileJson.optInt("availableBp", 0),
            xpRemainderForAp = profileJson.optDouble("xpRemainderForAp", 0.0),
            xpRemainderForBp = profileJson.optDouble("xpRemainderForBp", 0.0),
            avatarPath = if (profileJson.has("avatarPath") && !profileJson.isNull("avatarPath")) profileJson.getString("avatarPath") else null,
            heightInches = profileJson.optInt("heightInches", 78),
            weightLbs = profileJson.optInt("weightLbs", 210),
            jerseyNumber = profileJson.optInt("jerseyNumber", 23),
            teamName = profileJson.optString("teamName", "Draft Prospect"),
            createdAt = profileJson.optLong("createdAt", System.currentTimeMillis()),
            currentSeason = profileJson.optInt("currentSeason", 1),
            seasonStartOvr = profileJson.optInt("seasonStartOvr", profileJson.optInt("overallRating", 60)),
            isInPlayoffs = profileJson.optBoolean("isInPlayoffs", false),
            playoffBracketJson = if (profileJson.has("playoffBracketJson") && !profileJson.isNull("playoffBracketJson")) profileJson.getString("playoffBracketJson") else null,
            themeMode = profileJson.optString("themeMode", "DARK"),
            selectedTeamTheme = if (profileJson.has("selectedTeamTheme") && !profileJson.isNull("selectedTeamTheme")) profileJson.getString("selectedTeamTheme") else null,
            amoledBlack = profileJson.optBoolean("amoledBlack", false),
            customPrimaryHex = profileJson.optLong("customPrimaryHex", 0xFFFFB300),
            customSecondaryHex = profileJson.optLong("customSecondaryHex", 0xFF1E88E5),
            customBackgroundHex = profileJson.optLong("customBackgroundHex", 0xFF121212),
            customSurfaceHex = profileJson.optLong("customSurfaceHex", 0xFF1E1E1E),
            customTextHex = profileJson.optLong("customTextHex", 0xFFFFFFFF),
            weightAllowanceLbs = profileJson.optInt("weightAllowanceLbs", 0)
        )

        val newProfileId = database.playerProfileDao().insertProfile(importedProfile).toInt()

        // Attributes
        val attrArray = rootJson.optJSONArray("attributes")
        val attributesToInsert = mutableListOf<PlayerAttribute>()
        if (attrArray != null && attrArray.length() > 0) {
            for (i in 0 until attrArray.length()) {
                val item = attrArray.getJSONObject(i)
                attributesToInsert.add(
                    PlayerAttribute(
                        id = 0,
                        profileId = newProfileId,
                        category = item.optString("category", "General"),
                        attributeName = item.optString("attributeName", "Attribute"),
                        currentValue = item.optInt("currentValue", 60),
                        maxValue = item.optInt("maxValue", 99)
                    )
                )
            }
        } else {
            attributesToInsert.addAll(
                AttributeSeedData.ATTRIBUTES.map { attr ->
                    attr.copy(id = 0, profileId = newProfileId, currentValue = importedProfile.overallRating)
                }
            )
        }
        database.playerAttributeDao().insertAttributes(attributesToInsert)

        val insertedAttrs = database.playerAttributeDao().getAllAttributesList(newProfileId)
        val attrNameToIdMap = insertedAttrs.associate { it.attributeName to it.id }

        // Badges
        val badgeArray = rootJson.optJSONArray("badges")
        val badgesToInsert = mutableListOf<PlayerBadge>()
        if (badgeArray != null && badgeArray.length() > 0) {
            for (i in 0 until badgeArray.length()) {
                val item = badgeArray.getJSONObject(i)
                badgesToInsert.add(
                    PlayerBadge(
                        id = 0,
                        profileId = newProfileId,
                        badgeName = item.optString("badgeName", "Badge"),
                        tier = item.optString("tier", "None"),
                        equipped = item.optBoolean("equipped", false),
                        category = item.optString("category", "Other")
                    )
                )
            }
        } else {
            badgesToInsert.addAll(
                BadgeSeedData.BADGES.map { badge ->
                    badge.copy(id = 0, profileId = newProfileId)
                }
            )
        }
        database.playerBadgeDao().insertBadges(badgesToInsert)

        val insertedBadges = database.playerBadgeDao().getAllBadges(newProfileId)
        val badgeNameToIdMap = insertedBadges.associate { it.badgeName to it.id }

        // Game Logs
        val logArray = rootJson.optJSONArray("gameLogs")
        if (logArray != null && logArray.length() > 0) {
            val logsToInsert = mutableListOf<GameLog>()
            for (i in 0 until logArray.length()) {
                val item = logArray.getJSONObject(i)
                logsToInsert.add(
                    GameLog(
                        id = 0,
                        profileId = newProfileId,
                        seasonNumber = item.optInt("seasonNumber", 1),
                        datePlayed = item.optLong("datePlayed", System.currentTimeMillis()),
                        minutesPlayed = item.optInt("minutesPlayed", 0),
                        points = item.optInt("points", 0),
                        rebounds = item.optInt("rebounds", 0),
                        assists = item.optInt("assists", 0),
                        steals = item.optInt("steals", 0),
                        blocks = item.optInt("blocks", 0),
                        turnovers = item.optInt("turnovers", 0),
                        fieldGoalsAttempted = item.optInt("fieldGoalsAttempted", 0),
                        fieldGoalsMade = item.optInt("fieldGoalsMade", 0),
                        freeThrowsAttempted = item.optInt("freeThrowsAttempted", 0),
                        freeThrowsMade = item.optInt("freeThrowsMade", 0),
                        isWin = item.optBoolean("isWin", false),
                        isPlayoff = item.optBoolean("isPlayoff", false),
                        teammateGrade = item.optString("teammateGrade", "B"),
                        quarterMinutes = item.optInt("quarterMinutes", 12),
                        playerOfTheGame = item.optBoolean("playerOfTheGame", false),
                        playerOfTheWeek = item.optBoolean("playerOfTheWeek", false),
                        allStar = item.optBoolean("allStar", false),
                        majorAward = item.optBoolean("majorAward", false)
                    )
                )
            }
            database.gameLogDao().insertGameLogs(logsToInsert)
        }

        // Upgrade History
        val historyArray = rootJson.optJSONArray("upgradeHistory")
        if (historyArray != null && historyArray.length() > 0) {
            val historiesToInsert = mutableListOf<UpgradeHistory>()
            for (i in 0 until historyArray.length()) {
                val item = historyArray.getJSONObject(i)
                val entityType = item.optString("entityType", "ATTRIBUTE")
                val entityName = item.optString("entityName", "")
                val oldEntityId = item.optInt("entityId", 0)
                val newEntityId = if (entityType == "ATTRIBUTE") {
                    attrNameToIdMap[entityName] ?: oldEntityId
                } else {
                    badgeNameToIdMap[entityName] ?: oldEntityId
                }

                historiesToInsert.add(
                    UpgradeHistory(
                        id = 0,
                        profileId = newProfileId,
                        seasonNumber = item.optInt("seasonNumber", 1),
                        entityType = entityType,
                        entityId = newEntityId,
                        oldValue = item.optInt("oldValue", 0),
                        newValue = item.optInt("newValue", 0),
                        costSpent = item.optInt("costSpent", 0),
                        currencyType = item.optString("currencyType", "AP"),
                        isAppliedIn2k = item.optBoolean("isAppliedIn2k", false),
                        timestamp = item.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            database.upgradeHistoryDao().insertUpgradeHistory(historiesToInsert)
        }

        // Player Awards
        val awardArray = rootJson.optJSONArray("playerAwards")
        if (awardArray != null && awardArray.length() > 0) {
            val awardsToInsert = mutableListOf<com.example.playerlock.data.PlayerAward>()
            for (i in 0 until awardArray.length()) {
                val item = awardArray.getJSONObject(i)
                awardsToInsert.add(
                    com.example.playerlock.data.PlayerAward(
                        id = 0,
                        profileId = newProfileId,
                        awardName = item.optString("awardName", "Award"),
                        category = item.optString("category", "MAJOR"),
                        seasonNumber = item.optInt("seasonNumber", 1),
                        xpGranted = item.optInt("xpGranted", 0),
                        timestamp = item.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            database.playerAwardDao().insertAwards(awardsToInsert)
        }

        // Season Summaries
        val summaryArray = rootJson.optJSONArray("seasonSummaries")
        if (summaryArray != null && summaryArray.length() > 0) {
            val summariesToInsert = mutableListOf<SeasonSummary>()
            for (i in 0 until summaryArray.length()) {
                val item = summaryArray.getJSONObject(i)
                summariesToInsert.add(
                    SeasonSummary(
                        id = 0,
                        profileId = newProfileId,
                        seasonNumber = item.optInt("seasonNumber", 1),
                        wins = item.optInt("wins", 0),
                        losses = item.optInt("losses", 0),
                        gamesPlayed = item.optInt("gamesPlayed", 0),
                        mpg = item.optDouble("mpg", 0.0),
                        totalMinutes = item.optInt("totalMinutes", 0),
                        ppg = item.optDouble("ppg", 0.0),
                        rpg = item.optDouble("rpg", 0.0),
                        apg = item.optDouble("apg", 0.0),
                        spg = item.optDouble("spg", 0.0),
                        bpg = item.optDouble("bpg", 0.0),
                        fgPct = item.optDouble("fgPct", 0.0),
                        ftPct = item.optDouble("ftPct", 0.0),
                        doubleDoubles = item.optInt("doubleDoubles", 0),
                        tripleDoubles = item.optInt("tripleDoubles", 0),
                        seasonHighSummary = item.optString("seasonHighSummary", ""),
                        startOvr = item.optInt("startOvr", 60),
                        endOvr = item.optInt("endOvr", 60),
                        attributeUpgradesSummary = item.optString("attributeUpgradesSummary", ""),
                        badgeUpgradesSummary = item.optString("badgeUpgradesSummary", ""),
                        awardsSummary = item.optString("awardsSummary", ""),
                        totalXpEarned = item.optInt("totalXpEarned", 0),
                        timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                        playoffResult = item.optString("playoffResult", "")
                    )
                )
            }
            database.seasonSummaryDao().insertSeasonSummaries(summariesToInsert)
        }
        return newProfileId
    }
}
