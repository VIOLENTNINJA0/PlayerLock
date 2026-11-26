package com.example.playerlock.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.playerlock.progression.ProgressionRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

fun normalizeBadgeTier(tierRaw: String): String {
    return when (tierRaw.trim().uppercase(java.util.Locale.ROOT)) {
        "1", "BRONZE" -> "Bronze"
        "2", "SILVER" -> "Silver"
        "3", "GOLD" -> "Gold"
        "4", "HOF", "HALL OF FAME", "HALL_OF_FAME" -> "HOF"
        "5", "LEGEND" -> "Legend"
        else -> "None"
    }
}

data class CustomPlayerTemplate(
    val name: String = "Custom Player",
    val position: String = "PG",
    val heightInches: Int = 78,
    val weightLbs: Int = 210,
    val jerseyNumber: Int = 23,
    val teamName: String = "Draft Prospect",
    val archetype: String = "3PT Playmaker",
    val startingOvr: Int = 60,
    val attributes: Map<String, Int> = emptyMap(),
    val badges: Map<String, String> = emptyMap()
)

class CareerBackupManager(
    private val repository: ProgressionRepository,
    private val settingsDataStore: SettingsDataStore
) {

    /**
     * Serializes the active player profile along with all attributes, badges, game logs,
     * and upgrade history into a clean formatted JSON string.
     */
    suspend fun exportCareerToJson(profileId: Int): String? {
        val profile = repository.getProfile(profileId) ?: return null

        val attributes = repository.getAllAttributesList(profileId)
        val badges = repository.getAllBadges(profileId)
        val gameLogs = repository.getAllGameLogs(profileId)
        val upgradeHistory = repository.getAllUpgradeHistory(profileId)
        val awards = repository.getAllAwards(profileId)

        val rootJson = JSONObject()
        rootJson.put("version", 1)

        val profileJson = JSONObject().apply {
            put("playerName", profile.playerName)
            put("position", profile.position)
            put("overallRating", profile.overallRating)
            put("availableAp", profile.availableAp)
            put("availableBp", profile.availableBp)
            put("xpRemainderForAp", profile.xpRemainderForAp)
            put("xpRemainderForBp", profile.xpRemainderForBp)
            put("avatarPath", profile.avatarPath)
            put("heightInches", profile.heightInches)
            put("weightLbs", profile.weightLbs)
            put("jerseyNumber", profile.jerseyNumber)
            put("teamName", profile.teamName)
            put("createdAt", profile.createdAt)
            put("currentSeason", profile.currentSeason)
            put("seasonStartOvr", profile.seasonStartOvr)
            put("isInPlayoffs", profile.isInPlayoffs)
            profile.playoffBracketJson?.let { put("playoffBracketJson", it) }
        }
        rootJson.put("profile", profileJson)

        val attrArray = JSONArray()
        attributes.forEach { attr ->
            val attrJson = JSONObject().apply {
                put("category", attr.category)
                put("attributeName", attr.attributeName)
                put("currentValue", attr.currentValue)
                put("maxValue", attr.maxValue)
            }
            attrArray.put(attrJson)
        }
        rootJson.put("attributes", attrArray)

        val badgeArray = JSONArray()
        badges.forEach { badge ->
            val badgeJson = JSONObject().apply {
                put("badgeName", badge.badgeName)
                put("tier", badge.tier)
                put("equipped", badge.equipped)
                put("category", badge.category)
            }
            badgeArray.put(badgeJson)
        }
        rootJson.put("badges", badgeArray)

        val logArray = JSONArray()
        gameLogs.forEach { log ->
            val logJson = JSONObject().apply {
                put("seasonNumber", log.seasonNumber)
                put("datePlayed", log.datePlayed)
                put("minutesPlayed", log.minutesPlayed)
                put("points", log.points)
                put("rebounds", log.rebounds)
                put("assists", log.assists)
                put("steals", log.steals)
                put("blocks", log.blocks)
                put("turnovers", log.turnovers)
                put("fieldGoalsAttempted", log.fieldGoalsAttempted)
                put("fieldGoalsMade", log.fieldGoalsMade)
                put("freeThrowsAttempted", log.freeThrowsAttempted)
                put("freeThrowsMade", log.freeThrowsMade)
                put("isWin", log.isWin)
                put("isPlayoff", log.isPlayoff)
                put("teammateGrade", log.teammateGrade)
                put("quarterMinutes", log.quarterMinutes)
                put("playerOfTheGame", log.playerOfTheGame)
                put("playerOfTheWeek", log.playerOfTheWeek)
                put("allStar", log.allStar)
                put("majorAward", log.majorAward)
            }
            logArray.put(logJson)
        }
        rootJson.put("gameLogs", logArray)

        val attrMap = attributes.associateBy { it.id }
        val badgeMap = badges.associateBy { it.id }

        val historyArray = JSONArray()
        upgradeHistory.forEach { history ->
            val entityName = when (history.entityType) {
                "ATTRIBUTE" -> attrMap[history.entityId]?.attributeName ?: ""
                "BADGE" -> badgeMap[history.entityId]?.badgeName ?: ""
                else -> ""
            }
            val historyJson = JSONObject().apply {
                put("seasonNumber", history.seasonNumber)
                put("entityType", history.entityType)
                put("entityId", history.entityId)
                put("entityName", entityName)
                put("oldValue", history.oldValue)
                put("newValue", history.newValue)
                put("costSpent", history.costSpent)
                put("currencyType", history.currencyType)
                put("isAppliedIn2k", history.isAppliedIn2k)
                put("timestamp", history.timestamp)
            }
            historyArray.put(historyJson)
        }
        rootJson.put("upgradeHistory", historyArray)

        val awardArray = JSONArray()
        awards.forEach { award ->
            val awardJson = JSONObject().apply {
                put("awardName", award.awardName)
                put("category", award.category)
                put("seasonNumber", award.seasonNumber)
                put("xpGranted", award.xpGranted)
                put("timestamp", award.timestamp)
            }
            awardArray.put(awardJson)
        }
        rootJson.put("playerAwards", awardArray)

        val summaries = repository.getAllSeasonSummaries(profileId)
        val summaryArray = JSONArray()
        summaries.forEach { summary ->
            val summaryJson = JSONObject().apply {
                put("seasonNumber", summary.seasonNumber)
                put("wins", summary.wins)
                put("losses", summary.losses)
                put("gamesPlayed", summary.gamesPlayed)
                put("mpg", summary.mpg)
                put("totalMinutes", summary.totalMinutes)
                put("ppg", summary.ppg)
                put("rpg", summary.rpg)
                put("apg", summary.apg)
                put("spg", summary.spg)
                put("bpg", summary.bpg)
                put("fgPct", summary.fgPct)
                put("ftPct", summary.ftPct)
                put("doubleDoubles", summary.doubleDoubles)
                put("tripleDoubles", summary.tripleDoubles)
                put("seasonHighSummary", summary.seasonHighSummary)
                put("startOvr", summary.startOvr)
                put("endOvr", summary.endOvr)
                put("attributeUpgradesSummary", summary.attributeUpgradesSummary)
                put("badgeUpgradesSummary", summary.badgeUpgradesSummary)
                put("awardsSummary", summary.awardsSummary)
                put("totalXpEarned", summary.totalXpEarned)
                put("timestamp", summary.timestamp)
                put("playoffResult", summary.playoffResult)
            }
            summaryArray.put(summaryJson)
        }
        rootJson.put("seasonSummaries", summaryArray)

        return rootJson.toString(2)
    }

    /**
     * Parses the JSON string, creates a brand new profile (preventing overwrites),
     * inserts all related records under the new profile ID, sets activeProfileId,
     * and returns Result.success(newProfileId).
     */
    suspend fun importCareerFromJson(jsonString: String): Result<Int> {
        return try {
            val newProfileId = repository.importCareerFromJson(jsonString)
            settingsDataStore.setActiveProfileId(newProfileId)
            Result.success(newProfileId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Performs a background auto-save of the active player profile JSON.
     * Always writes a rolling internal snapshot inside a player subfolder,
     * and writes to external selected folder URI inside the player subfolder if configured.
     */
    suspend fun performAutoSaveIfEnabled(profileId: Int) {
        try {
            val jsonString = exportCareerToJson(profileId) ?: return
            val profile = repository.getProfile(profileId)
            val rawName = profile?.playerName ?: "Player_$profileId"
            val safePlayerName = rawName.trim().replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "Player_${profile?.id ?: profileId}" }

            // 1. Internal Fallback Snapshot Subdirectory
            try {
                val internalFile = File(settingsDataStore.context.filesDir, "autosaves/$safePlayerName/${safePlayerName}_autosave.json")
                internalFile.parentFile?.mkdirs()
                internalFile.writeText(jsonString, Charsets.UTF_8)
            } catch (_: Exception) {
                // Ignore internal snapshot write errors
            }

            // 2. External Backup Folder Subdirectory
            val settings = settingsDataStore.loadSettings()
            if (settings.autoSaveEnabled && !settings.autoSaveFolderUri.isNullOrBlank()) {
                val context = settingsDataStore.context
                val treeUri = Uri.parse(settings.autoSaveFolderUri)
                val rootDir = DocumentFile.fromTreeUri(context, treeUri)
                if (rootDir != null && rootDir.exists() && rootDir.isDirectory) {
                    val playerDir = rootDir.findFile(safePlayerName)?.takeIf { it.isDirectory }
                        ?: rootDir.createDirectory(safePlayerName)
                    if (playerDir != null && playerDir.exists() && playerDir.isDirectory) {
                        val fileName = "${safePlayerName}_autosave.json"
                        val autoSaveFile = playerDir.findFile(fileName)
                            ?: playerDir.createFile("application/json", fileName)
                        if (autoSaveFile != null) {
                            context.contentResolver.openOutputStream(autoSaveFile.uri, "wt")?.use { out ->
                                out.write(jsonString.toByteArray(Charsets.UTF_8))
                            }
                            val now = System.currentTimeMillis()
                            settingsDataStore.saveLastAutoSaveTimestamp(now)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Catch SecurityException, IOException, etc. safely so auto-save never crashes the app
        }
    }

    fun isCareerSaveJson(jsonString: String): Boolean {
        return try {
            val json = JSONObject(jsonString)
            json.has("profile") && json.has("attributes") && json.has("gameLogs")
        } catch (_: Exception) {
            false
        }
    }

    fun exportCustomPlayerTemplateToJson(template: CustomPlayerTemplate): String {
        val rootJson = JSONObject().apply {
            put("type", "custom_player_template")
            put("version", 1)
            put("name", template.name)
            put("position", template.position)
            put("heightInches", template.heightInches)
            put("weightLbs", template.weightLbs)
            put("jerseyNumber", template.jerseyNumber)
            put("teamName", template.teamName)
            put("archetype", template.archetype)
            put("startingOvr", template.startingOvr)

            val attrJson = JSONObject()
            template.attributes.forEach { (k, v) -> attrJson.put(k, v) }
            put("attributes", attrJson)

            val badgeJson = JSONObject()
            template.badges.forEach { (k, v) -> badgeJson.put(k, v) }
            put("badges", badgeJson)
        }
        return rootJson.toString(2)
    }

    fun parseCustomPlayerTemplateJson(jsonString: String): CustomPlayerTemplate {
        val json = JSONObject(jsonString)
        val name = json.optString("name", json.optString("playerName", "Custom Player"))
        val position = json.optString("position", "PG")
        val heightInches = json.optInt("heightInches", 78)
        val weightLbs = json.optInt("weightLbs", 210)
        val jerseyNumber = json.optInt("jerseyNumber", 23)
        val teamName = json.optString("teamName", "Draft Prospect")
        val archetype = json.optString("archetype", "3PT Playmaker")
        val startingOvr = json.optInt("startingOvr", 60)

        val attributesMap = mutableMapOf<String, Int>()
        if (json.has("attributes")) {
            val attrObj = json.optJSONObject("attributes")
            if (attrObj != null) {
                attrObj.keys().forEach { key ->
                    attributesMap[key] = attrObj.optInt(key, 60)
                }
            } else {
                val attrArr = json.optJSONArray("attributes")
                if (attrArr != null) {
                    for (i in 0 until attrArr.length()) {
                        val item = attrArr.optJSONObject(i) ?: continue
                        val attrName = item.optString("attributeName", item.optString("name", ""))
                        val valNum = item.optInt("currentValue", item.optInt("value", 60))
                        if (attrName.isNotBlank()) {
                            attributesMap[attrName] = valNum
                        }
                    }
                }
            }
        }

        val badgesMap = mutableMapOf<String, String>()
        if (json.has("badges")) {
            val badgeObj = json.optJSONObject("badges")
            if (badgeObj != null) {
                badgeObj.keys().forEach { key ->
                    val valStr = badgeObj.optString(key, "None")
                    badgesMap[key] = normalizeBadgeTier(valStr)
                }
            } else {
                val badgeArr = json.optJSONArray("badges")
                if (badgeArr != null) {
                    for (i in 0 until badgeArr.length()) {
                        val item = badgeArr.optJSONObject(i) ?: continue
                        val badgeName = item.optString("badgeName", item.optString("name", ""))
                        val tier = item.optString("tier", "None")
                        if (badgeName.isNotBlank()) {
                            badgesMap[badgeName] = normalizeBadgeTier(tier)
                        }
                    }
                }
            }
        }

        return CustomPlayerTemplate(
            name = name,
            position = position,
            heightInches = heightInches,
            weightLbs = weightLbs,
            jerseyNumber = jerseyNumber,
            teamName = teamName,
            archetype = archetype,
            startingOvr = startingOvr,
            attributes = attributesMap,
            badges = badgesMap
        )
    }

    fun parsePlayerSetupFromJson(jsonString: String): Result<CustomPlayerTemplate> {
        return try {
            val json = JSONObject(jsonString)
            if (json.has("profile")) {
                val profileObj = json.getJSONObject("profile")
                val name = profileObj.optString("playerName", "Custom Player")
                val position = profileObj.optString("position", "PG")
                val heightInches = profileObj.optInt("heightInches", 78)
                val weightLbs = profileObj.optInt("weightLbs", 210)
                val jerseyNumber = profileObj.optInt("jerseyNumber", 23)
                val teamName = profileObj.optString("teamName", "Draft Prospect")
                val archetype = profileObj.optString("archetype", "3PT Playmaker")
                val startingOvr = profileObj.optInt("overallRating", profileObj.optInt("seasonStartOvr", 60))

                val attributesMap = mutableMapOf<String, Int>()
                val attrArray = json.optJSONArray("attributes")
                if (attrArray != null) {
                    for (i in 0 until attrArray.length()) {
                        val item = attrArray.optJSONObject(i) ?: continue
                        val attrName = item.optString("attributeName", item.optString("name", ""))
                        val valNum = item.optInt("currentValue", item.optInt("value", 60))
                        if (attrName.isNotBlank()) {
                            attributesMap[attrName] = valNum
                        }
                    }
                }

                val badgesMap = mutableMapOf<String, String>()
                val badgeArray = json.optJSONArray("badges")
                if (badgeArray != null) {
                    for (i in 0 until badgeArray.length()) {
                        val item = badgeArray.optJSONObject(i) ?: continue
                        val badgeName = item.optString("badgeName", item.optString("name", ""))
                        val tier = item.optString("tier", "None")
                        if (badgeName.isNotBlank()) {
                            badgesMap[badgeName] = normalizeBadgeTier(tier)
                        }
                    }
                }

                Result.success(
                    CustomPlayerTemplate(
                        name = name,
                        position = position,
                        heightInches = heightInches,
                        weightLbs = weightLbs,
                        jerseyNumber = jerseyNumber,
                        teamName = teamName,
                        archetype = archetype,
                        startingOvr = startingOvr,
                        attributes = attributesMap,
                        badges = badgesMap
                    )
                )
            } else {
                Result.success(parseCustomPlayerTemplateJson(jsonString))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parsePlayerSetupFromUri(context: Context, uri: Uri): Result<CustomPlayerTemplate> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }
            if (jsonString.isNullOrBlank()) {
                Result.failure(IllegalArgumentException("Selected JSON file is empty."))
            } else {
                parsePlayerSetupFromJson(jsonString)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
