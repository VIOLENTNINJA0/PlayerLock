package com.example.playerlock.progression

import com.example.playerlock.data.AttributeSeedData
import com.example.playerlock.data.PlayerAttribute
import com.example.playerlock.data.PlayerBadge
import com.example.playerlock.data.PlayerProfile
import com.example.playerlock.data.UpgradeHistory
import kotlin.math.roundToInt

object ProgressionCalculator {

    data class GameInputs(
        val points: Int,
        val rebounds: Int,
        val assists: Int,
        val steals: Int,
        val blocks: Int,
        val turnovers: Int,
        val fieldGoalsAttempted: Int,
        val fieldGoalsMade: Int,
        val isWin: Boolean,
        val isPlayoff: Boolean,
        val teammateGrade: String,
        val quarterMinutes: Int,
        val useQuarterNormalizer: Boolean,
        val isDoubleDouble: Boolean,
        val isTripleDouble: Boolean,
        val isPlayerOfTheGame: Boolean,
        val isPlayerOfTheWeek: Boolean,
        val isAllStar: Boolean,
        val isMajorAward: Boolean
    )

    data class GameXpResult(
        val rawStatXp: Double,
        val winMultiplier: Double,
        val teammateGradeMultiplier: Double,
        val efficiencyMultiplier: Double,
        val quarterNormalizer: Double,
        val milestoneXp: Int,
        val totalXp: Double,
        val apGained: Int,
        val bpGained: Int,
        val xpRemainderForAp: Double,
        val xpRemainderForBp: Double
    )

    data class UpgradeCost(
        val cost: Int,
        val currency: String
    )

    fun calculateGameXp(
        inputs: GameInputs,
        carriedXpRemainderForAp: Double,
        carriedXpRemainderForBp: Double
    ): GameXpResult {
        val rawStatXp = coerceAtLeastZero(
            inputs.points * 1.0 +
                inputs.rebounds * 1.2 +
                inputs.assists * 1.5 +
                inputs.steals * 2.5 +
                inputs.blocks * 2.5 -
                inputs.turnovers * 1.5
        )
        val winMultiplier = when {
            inputs.isPlayoff && inputs.isWin -> 1.35
            inputs.isWin -> 1.25
            else -> 1.0
        }
        val tgMult = teammateGradeMultiplier(inputs.teammateGrade)
        val effMult = efficiencyMultiplier(inputs.fieldGoalsAttempted, inputs.fieldGoalsMade)
        val qNorm = if (inputs.useQuarterNormalizer) {
            coerceAtLeastOne((12.0 / inputs.quarterMinutes) * 0.85)
        } else 1.0
        var milestoneXp = 0
        if (inputs.isTripleDouble) {
            milestoneXp += 35
        } else if (inputs.isDoubleDouble) {
            milestoneXp += 15
        }
        if (inputs.isPlayerOfTheGame) milestoneXp += 20
        if (inputs.isPlayerOfTheWeek) milestoneXp += 50
        if (inputs.isAllStar) milestoneXp += 150
        if (inputs.isMajorAward) milestoneXp += 500
        val totalXp = (rawStatXp * winMultiplier * tgMult * effMult * qNorm) + milestoneXp
        val xpPool = totalXp + carriedXpRemainderForAp + carriedXpRemainderForBp
        val apGained = (xpPool / 25).toInt()
        val xpRemainderForAp = xpPool % 25
        val bpGained = (xpPool / 100).toInt()
        val xpRemainderForBp = xpPool % 100
        return GameXpResult(
            rawStatXp = rawStatXp,
            winMultiplier = winMultiplier,
            teammateGradeMultiplier = tgMult,
            efficiencyMultiplier = effMult,
            quarterNormalizer = qNorm,
            milestoneXp = milestoneXp,
            totalXp = totalXp,
            apGained = apGained,
            bpGained = bpGained,
            xpRemainderForAp = xpRemainderForAp,
            xpRemainderForBp = xpRemainderForBp
        )
    }

    fun attributeUpgradeCost(currentValue: Int): UpgradeCost {
        val cost = when {
            currentValue < 75 -> 1
            currentValue in 75..84 -> 2
            currentValue in 85..92 -> 4
            currentValue in 93..99 -> 6
            else -> 6
        }
        return UpgradeCost(cost, "AP")
    }

    fun badgeUpgradeCost(targetTier: String): UpgradeCost {
        val cost = when (targetTier.uppercase()) {
            "BRONZE" -> 1
            "SILVER" -> 2
            "GOLD" -> 3
            "HOF" -> 5
            "LEGEND" -> 8
            else -> 0
        }
        return UpgradeCost(cost, "BP")
    }

    fun canAfford(profile: PlayerProfile, cost: UpgradeCost): Boolean {
        return when (cost.currency) {
            "AP" -> profile.availableAp >= cost.cost
            "BP" -> profile.availableBp >= cost.cost
            else -> false
        }
    }

    fun canUpgradeAttribute(currentValue: Int): Boolean = currentValue < 99

    fun canApplyBadge(tier: String): Boolean {
        return listOf("BRONZE", "SILVER", "GOLD", "HOF", "LEGEND").contains(tier.uppercase())
    }

    fun applyAttributeUpgrade(
        attribute: PlayerAttribute,
        profile: PlayerProfile,
        newValue: Int
    ): Pair<PlayerAttribute, UpgradeHistory>? {
        if (!canUpgradeAttribute(attribute.currentValue)) return null
        if (newValue <= attribute.currentValue) return null
        if (newValue > attribute.maxValue) return null
        val cost = attributeUpgradeCost(attribute.currentValue)
        if (!canAfford(profile, cost)) return null
        val updatedAttribute = attribute.copy(currentValue = newValue)
        val history = UpgradeHistory(
            profileId = attribute.profileId,
            entityType = "ATTRIBUTE",
            entityId = attribute.id,
            oldValue = attribute.currentValue,
            newValue = newValue,
            costSpent = cost.cost,
            currencyType = cost.currency,
            isAppliedIn2k = false,
            timestamp = System.currentTimeMillis()
        )
        return Pair(updatedAttribute, history)
    }

    fun applyBadgeUpgrade(
        badge: PlayerBadge,
        profile: PlayerProfile,
        targetTier: String
    ): Pair<PlayerBadge, UpgradeHistory>? {
        if (!canApplyBadge(targetTier)) return null
        val cost = badgeUpgradeCost(targetTier)
        if (!canAfford(profile, cost)) return null
        val updatedBadge = badge.copy(tier = targetTier.replaceFirstChar { it.uppercase() })
        val history = UpgradeHistory(
            profileId = badge.profileId,
            entityType = "BADGE",
            entityId = badge.id,
            oldValue = badge.tier.let { if (it == "None") 0 else tierToNumeric(it) },
            newValue = tierToNumeric(targetTier),
            costSpent = cost.cost,
            currencyType = cost.currency,
            isAppliedIn2k = false,
            timestamp = System.currentTimeMillis()
        )
        return Pair(updatedBadge, history)
    }

    data class UndoResult(
        val success: Boolean,
        val refundedAp: Int,
        val refundedBp: Int,
        val message: String
    )

    fun undoUpgrade(
        history: UpgradeHistory,
        profile: PlayerProfile,
        attribute: PlayerAttribute?,
        badge: PlayerBadge?
    ): UndoResult {
        if (history.isAppliedIn2k) {
            return UndoResult(false, 0, 0, "Cannot undo an upgrade already applied in 2K.")
        }
        when (history.entityType) {
            "ATTRIBUTE" -> {
                if (attribute == null || attribute.id != history.entityId) {
                    return UndoResult(false, 0, 0, "Attribute ID mismatch.")
                }
            }
            "BADGE" -> {
                if (badge == null || badge.id != history.entityId) {
                    return UndoResult(false, 0, 0, "Badge ID mismatch.")
                }
            }
            else -> return UndoResult(false, 0, 0, "Unknown entity type: ")
        }
        val refundedAp = if (history.currencyType == "AP") history.costSpent else 0
        val refundedBp = if (history.currencyType == "BP") history.costSpent else 0
        return UndoResult(true, refundedAp, refundedBp, "Undo successful.")
    }

    // -----------------------------------------------------------------------
    // Position-Weighted NBA 2K OVR Calculator
    // -----------------------------------------------------------------------

    private val POSITION_ATTRIBUTE_WEIGHTS: Map<String, Map<String, Double>> = mapOf(
        "PG" to mapOf(
            // Heavy (3.0x)
            "Ball Handle" to 3.0,
            "Pass Accuracy" to 3.0,
            "Speed with Ball" to 3.0,
            "Pass IQ" to 3.0,
            "Pass Vision" to 3.0,
            "Three-Point Shot" to 3.0,
            "Mid-Range Shot" to 3.0,
            "Driving Layup" to 3.0,
            "Perimeter Defense" to 3.0,
            "Speed" to 3.0,
            "Agility" to 3.0,
            // Medium (1.5x)
            "Close Shot" to 1.5,
            "Free Throw" to 1.5,
            "Shot IQ" to 1.5,
            "Offensive Consistency" to 1.5,
            "Steal" to 1.5,
            "Defensive Consistency" to 1.5,
            "Stamina" to 1.5,
            // Low (0.4x)
            "Standing Dunk" to 0.4,
            "Post Hook" to 0.4,
            "Post Control" to 0.4,
            "Block" to 0.4,
            "Offensive Rebound" to 0.4,
            // Misc / low weight (0.35x)
            "Intangibles" to 0.35,
            "Potential" to 0.0
        ),
        "SG" to mapOf(
            // Heavy (3.0x)
            "Three-Point Shot" to 3.0,
            "Mid-Range Shot" to 3.0,
            "Driving Layup" to 3.0,
            "Driving Dunk" to 3.0,
            "Ball Handle" to 3.0,
            "Perimeter Defense" to 3.0,
            "Speed" to 3.0,
            "Agility" to 3.0,
            "Shot IQ" to 3.0,
            "Offensive Consistency" to 3.0,
            // Medium (1.5x)
            "Close Shot" to 1.5,
            "Free Throw" to 1.5,
            "Pass Accuracy" to 1.5,
            "Speed with Ball" to 1.5,
            "Steal" to 1.5,
            "Defensive Consistency" to 1.5,
            "Vertical" to 1.5,
            "Stamina" to 1.5,
            // Low (0.4x)
            "Post Hook" to 0.4,
            "Post Control" to 0.4,
            "Standing Dunk" to 0.4,
            "Offensive Rebound" to 0.4,
            // Misc / low weight (0.35x)
            "Intangibles" to 0.35,
            "Potential" to 0.0
        ),
        "SF" to mapOf(
            // Balanced (2.5x)
            "Three-Point Shot" to 2.5,
            "Mid-Range Shot" to 2.5,
            "Driving Layup" to 2.5,
            "Driving Dunk" to 2.5,
            "Perimeter Defense" to 2.5,
            "Interior Defense" to 2.5,
            "Steal" to 2.5,
            "Defensive Rebound" to 2.5,
            "Speed" to 2.5,
            "Agility" to 2.5,
            "Strength" to 2.5,
            "Vertical" to 2.5,
            "Offensive Consistency" to 2.5,
            "Defensive Consistency" to 2.5,
            // Medium (1.5x)
            "Close Shot" to 1.5,
            "Ball Handle" to 1.5,
            "Pass Accuracy" to 1.5,
            "Block" to 1.5,
            "Hustle" to 1.5,
            "Stamina" to 1.5,
            // Low (0.6x)
            "Post Hook" to 0.6,
            // Misc / low weight (0.35x)
            "Intangibles" to 0.35,
            "Potential" to 0.0
        ),
        "PF" to mapOf(
            // Heavy (3.0x)
            "Close Shot" to 3.0,
            "Mid-Range Shot" to 3.0,
            "Standing Dunk" to 3.0,
            "Driving Dunk" to 3.0,
            "Post Control" to 3.0,
            "Interior Defense" to 3.0,
            "Block" to 3.0,
            "Offensive Rebound" to 3.0,
            "Defensive Rebound" to 3.0,
            "Strength" to 3.0,
            "Vertical" to 3.0,
            // Medium (1.5x)
            "Three-Point Shot" to 1.5,
            "Post Fade" to 1.5,
            "Perimeter Defense" to 1.5,
            "Help Defense IQ" to 1.5,
            "Defensive Consistency" to 1.5,
            "Speed" to 1.5,
            "Hustle" to 1.5,
            "Stamina" to 1.5,
            // Low (0.5x)
            "Ball Handle" to 0.5,
            "Pass Vision" to 0.5,
            "Speed with Ball" to 0.5,
            // Misc / low weight (0.35x)
            "Intangibles" to 0.35,
            "Potential" to 0.0
        ),
        "C" to mapOf(
            // Heavy (3.0x)
            "Close Shot" to 3.0,
            "Standing Dunk" to 3.0,
            "Post Control" to 3.0,
            "Post Hook" to 3.0,
            "Interior Defense" to 3.0,
            "Block" to 3.0,
            "Offensive Rebound" to 3.0,
            "Defensive Rebound" to 3.0,
            "Strength" to 3.0,
            "Offensive Consistency" to 3.0,
            "Defensive Consistency" to 3.0,
            // Medium (1.5x)
            "Driving Dunk" to 1.5,
            "Mid-Range Shot" to 1.5,
            "Free Throw" to 1.5,
            "Pass Accuracy" to 1.5,
            "Help Defense IQ" to 1.5,
            "Vertical" to 1.5,
            "Stamina" to 1.5,
            // Low (0.3x)
            "Ball Handle" to 0.3,
            "Speed with Ball" to 0.3,
            "Three-Point Shot" to 0.3,
            "Steal" to 0.3,
            // Misc / low weight (0.35x)
            "Intangibles" to 0.35,
            "Potential" to 0.0
        )
    )

    fun calculateOverallRating(position: String, attributes: Map<String, Int>): Int {
        if (attributes.isEmpty()) return 60

        val posWeights = POSITION_ATTRIBUTE_WEIGHTS[position] ?: POSITION_ATTRIBUTE_WEIGHTS["PG"] ?: emptyMap()

        var weightedSum = 0.0
        var totalWeight = 0.0

        for (attr in AttributeSeedData.ATTRIBUTES) {
            val name = attr.attributeName
            val value = attributes[name] ?: 60
            val weight = posWeights[name] ?: 1.0

            weightedSum += value * weight
            totalWeight += weight
        }

        if (totalWeight <= 0.0) return 60
        val baseOvr = weightedSum / totalWeight

        // Top-end star curve (+2 to +4 when top 5 weighted attributes average >= 88)
        val heavyAttrs = AttributeSeedData.ATTRIBUTES
            .filter { (posWeights[it.attributeName] ?: 1.0) >= 2.5 }
            .map { attributes[it.attributeName] ?: 60 }
            .sortedDescending()

        val top5 = if (heavyAttrs.size >= 5) heavyAttrs.take(5) else heavyAttrs
        val starBonus = if (top5.isNotEmpty()) {
            val top5Avg = top5.average()
            when {
                top5Avg >= 93.0 -> 4
                top5Avg >= 90.0 -> 3
                top5Avg >= 88.0 -> 2
                else -> 0
            }
        } else {
            0
        }

        return (baseOvr + starBonus).roundToInt().coerceIn(60, 99)
    }

    fun calculateOverallRating(position: String, attributes: List<PlayerAttribute>): Int {
        val attrMap = attributes.associate { it.attributeName to it.currentValue }
        return calculateOverallRating(position, attrMap)
    }

    // -----------------------------------------------------------------------
    // Performance Rating / Grade Calculation
    // -----------------------------------------------------------------------

    fun calculatePerformanceGrade(
        points: Int,
        rebounds: Int,
        assists: Int,
        steals: Int,
        blocks: Int,
        turnovers: Int,
        fieldGoalsAttempted: Int,
        fieldGoalsMade: Int
    ): String {
        val missedFg = (fieldGoalsAttempted - fieldGoalsMade).coerceAtLeast(0)
        val impactScore = points +
            (rebounds * 1.2) +
            (assists * 1.5) +
            (steals * 2.2) +
            (blocks * 2.2) -
            (turnovers * 1.8) -
            (missedFg * 0.6)

        return when {
            impactScore >= 42.0 -> "A+"
            impactScore >= 34.0 -> "A"
            impactScore >= 27.0 -> "A-"
            impactScore >= 21.0 -> "B+"
            impactScore >= 15.0 -> "B"
            impactScore >= 10.0 -> "B-"
            impactScore >= 6.0 -> "C+"
            impactScore >= 2.0 -> "C"
            else -> "D/F"
        }
    }

    private fun teammateGradeMultiplier(grade: String): Double {
        return when (grade.uppercase()) {
            "A+" -> 1.25
            "A", "A-" -> 1.15
            "B+", "B", "B-" -> 1.0
            else -> 0.85
        }
    }

    private fun efficiencyMultiplier(fga: Int, fgm: Int): Double {
        if (fga < 5) return 1.0
        val fgPct = fgm.toDouble() / fga
        return when {
            fgPct >= 0.50 -> 1.05
            fgPct >= 0.45 -> 1.0
            else -> {
                val penalty = coerceAtMost(0.30, (0.45 - fgPct) * 1.5)
                coerceAtLeast(0.70, 1.0 - penalty)
            }
        }
    }

    private fun tierToNumeric(tier: String): Int {
        return when (tier.uppercase()) {
            "NONE" -> 0
            "BRONZE" -> 1
            "SILVER" -> 2
            "GOLD" -> 3
            "HOF" -> 4
            "LEGEND" -> 5
            else -> 0
        }
    }

    private fun coerceAtLeastZero(value: Double): Double = if (value < 0) 0.0 else value
    private fun coerceAtLeastOne(value: Double): Double = if (value < 1.0) 1.0 else value
    private fun coerceAtMost(max: Double, value: Double): Double = if (value > max) max else value
    private fun coerceAtLeast(min: Double, value: Double): Double = if (value < min) min else value
}
