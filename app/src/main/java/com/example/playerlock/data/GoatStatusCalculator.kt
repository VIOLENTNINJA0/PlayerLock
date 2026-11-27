package com.example.playerlock.data

import java.util.Locale

data class GoatTierInfo(
    val tierNumber: Int,          // 10 to 1
    val tierName: String,          // e.g. "Rookie Prospect"
    val minPoints: Int,           // e.g. 0
    val maxPoints: Int,           // e.g. 499
    val nextTierName: String?,    // e.g. "Starter"
    val nextTierPoints: Int?,     // e.g. 500
    val featuredLegend: String?   // e.g. "Carmelo Anthony"
)

data class GoatPointsBreakdown(
    val championshipPts: Int,
    val finalsMvpPts: Int,
    val mvpPts: Int,
    val dpoyPts: Int,
    val allNbaPts: Int,
    val royPts: Int,
    val allStarPts: Int,
    val otherAwardsPts: Int,
    val careerStatsPts: Int,
    val tripleDoublePts: Int,
    val fiftyPointPts: Int,
    val totalPoints: Int
)

data class GoatStatusResult(
    val goatPoints: Int,
    val currentTier: GoatTierInfo,
    val nextTier: GoatTierInfo?,
    val progressToNextTier: Float, // 0.0 to 1.0
    val breakdown: GoatPointsBreakdown
)

data class NbaLegendRecord(
    val rank: Int,
    val playerName: String,
    val statValue: Long,
    val isUser: Boolean = false
)

data class RecordBookCategory(
    val categoryName: String,     // "Points", "Rebounds", "Assists", "Steals", "Blocks"
    val userStatValue: Long,
    val userRank: Int,            // e.g. 11 or 4
    val rankings: List<NbaLegendRecord>,
    val nextLegendName: String?,
    val statsToNextLegend: Long
)

object GoatStatusCalculator {

    val TIERS = listOf(
        GoatTierInfo(10, "Rookie Prospect", 0, 499, "Starter", 500, null),
        GoatTierInfo(9, "Starter", 500, 1499, "All-Star Caliber", 1500, "Carmelo Anthony"),
        GoatTierInfo(8, "All-Star Caliber", 1500, 2999, "Franchise Cornerstone", 3000, "Dirk Nowitzki"),
        GoatTierInfo(7, "Franchise Cornerstone", 3000, 4999, "Hall of Fame Lock", 5000, "Kevin Durant"),
        GoatTierInfo(6, "Hall of Fame Lock", 5000, 7499, "Top 10 All-Time", 7500, "Tim Duncan"),
        GoatTierInfo(5, "Top 10 All-Time", 7500, 10499, "Mount Rushmore", 10500, "Kobe Bryant"),
        GoatTierInfo(4, "Mount Rushmore", 10500, 13999, "Basketball Icon", 14000, "Kareem Abdul-Jabbar"),
        GoatTierInfo(3, "Basketball Icon", 14000, 17999, "Co-GOAT", 18000, "LeBron James"),
        GoatTierInfo(2, "Co-GOAT", 18000, 21999, "#1 Undisputed GOAT", 22000, "Michael Jordan"),
        GoatTierInfo(1, "#1 Undisputed GOAT", 22000, Int.MAX_VALUE, null, null, null)
    )

    fun calculateGoatStatus(
        gameLogs: List<GameLog>,
        awards: List<PlayerAward>
    ): GoatStatusResult {
        var champPts = 0
        var fMvpPts = 0
        var mvpPts = 0
        var dpoyPts = 0
        var allNbaPts = 0
        var royPts = 0
        var allStarPts = 0
        var otherAwardsPts = 0

        awards.forEach { award ->
            val nameUpper = award.awardName.uppercase(Locale.ROOT)
            when {
                nameUpper.contains("FINALS MVP") -> fMvpPts += 1000
                nameUpper.contains("CHAMPION") || nameUpper.contains("TITLE") -> champPts += 1500
                nameUpper.contains("MVP") || nameUpper.contains("MOST VALUABLE") -> mvpPts += 1200
                nameUpper.contains("DPOY") || nameUpper.contains("DEFENSIVE PLAYER") -> dpoyPts += 600
                nameUpper.contains("ALL-NBA 1ST") || nameUpper.contains("ALL-NBA FIRST") -> allNbaPts += 400
                nameUpper.contains("ALL-NBA 2ND") || nameUpper.contains("ALL-NBA SECOND") -> allNbaPts += 250
                nameUpper.contains("ALL-NBA 3RD") || nameUpper.contains("ALL-NBA THIRD") -> allNbaPts += 150
                nameUpper.contains("ROOKIE OF THE YEAR") || nameUpper.contains("ROY") -> royPts += 250
                nameUpper.contains("ALL-STAR") -> allStarPts += 150
                else -> otherAwardsPts += 100
            }
        }

        val totalPts = gameLogs.sumOf { it.points }
        val totalReb = gameLogs.sumOf { it.rebounds }
        val totalAst = gameLogs.sumOf { it.assists }
        val totalStl = gameLogs.sumOf { it.steals }
        val totalBlk = gameLogs.sumOf { it.blocks }

        val baseStatPts = (totalPts / 100) + (totalReb / 50) + (totalAst / 50) + ((totalStl + totalBlk) / 25)

        val tripleDoubles = gameLogs.count { log ->
            val d1 = if (log.points >= 10) 1 else 0
            val d2 = if (log.rebounds >= 10) 1 else 0
            val d3 = if (log.assists >= 10) 1 else 0
            val d4 = if (log.steals >= 10) 1 else 0
            val d5 = if (log.blocks >= 10) 1 else 0
            (d1 + d2 + d3 + d4 + d5) >= 3
        }
        val tripleDoublePts = tripleDoubles * 25

        val fiftyPtGames = gameLogs.count { it.points >= 50 }
        val fiftyPointPts = fiftyPtGames * 25

        val totalGoatPts = champPts + fMvpPts + mvpPts + dpoyPts + allNbaPts +
                royPts + allStarPts + otherAwardsPts + baseStatPts + tripleDoublePts + fiftyPointPts

        val currentTier = TIERS.lastOrNull { totalGoatPts >= it.minPoints } ?: TIERS.first()
        val nextTier = TIERS.firstOrNull { it.tierNumber == currentTier.tierNumber - 1 }

        val progress = if (nextTier != null) {
            val range = (nextTier.minPoints - currentTier.minPoints).toFloat()
            val earned = (totalGoatPts - currentTier.minPoints).toFloat()
            if (range > 0) (earned / range).coerceIn(0f, 1f) else 1f
        } else {
            1f
        }

        val breakdown = GoatPointsBreakdown(
            championshipPts = champPts,
            finalsMvpPts = fMvpPts,
            mvpPts = mvpPts,
            dpoyPts = dpoyPts,
            allNbaPts = allNbaPts,
            royPts = royPts,
            allStarPts = allStarPts,
            otherAwardsPts = otherAwardsPts,
            careerStatsPts = baseStatPts,
            tripleDoublePts = tripleDoublePts,
            fiftyPointPts = fiftyPointPts,
            totalPoints = totalGoatPts
        )

        return GoatStatusResult(
            goatPoints = totalGoatPts,
            currentTier = currentTier,
            nextTier = nextTier,
            progressToNextTier = progress,
            breakdown = breakdown
        )
    }

    // Top 10 All-Time NBA Record Books Data
    private val POINTS_LEADERBOARD = listOf(
        "LeBron James" to 43697L,
        "Kareem Abdul-Jabbar" to 38387L,
        "Karl Malone" to 36928L,
        "Kobe Bryant" to 33643L,
        "Kevin Durant" to 33006L,
        "Michael Jordan" to 32292L,
        "Dirk Nowitzki" to 31560L,
        "Wilt Chamberlain" to 31419L,
        "Julius Erving" to 30026L,
        "James Harden" to 29682L
    )

    private val REBOUNDS_LEADERBOARD = listOf(
        "Wilt Chamberlain" to 23924L,
        "Bill Russell" to 21620L,
        "Kareem Abdul-Jabbar" to 17440L,
        "Elvin Hayes" to 16279L,
        "Moses Malone" to 16212L,
        "Tim Duncan" to 15091L,
        "Karl Malone" to 14968L,
        "Robert Parish" to 14715L,
        "Kevin Garnett" to 14662L,
        "Dwight Howard" to 14627L
    )

    private val ASSISTS_LEADERBOARD = listOf(
        "John Stockton" to 15806L,
        "Chris Paul" to 12552L,
        "LeBron James" to 12119L,
        "Jason Kidd" to 12091L,
        "Russell Westbrook" to 10351L,
        "Steve Nash" to 10335L,
        "Mark Jackson" to 10334L,
        "Magic Johnson" to 10141L,
        "Oscar Robertson" to 9887L,
        "Isiah Thomas" to 9061L
    )

    private val STEALS_LEADERBOARD = listOf(
        "John Stockton" to 3265L,
        "Chris Paul" to 2728L,
        "Jason Kidd" to 2684L,
        "Michael Jordan" to 2514L,
        "Gary Payton" to 2445L,
        "LeBron James" to 2434L,
        "Maurice Cheeks" to 2310L,
        "Scottie Pippen" to 2307L,
        "Julius Erving" to 2272L,
        "Clyde Drexler" to 2207L
    )

    private val BLOCKS_LEADERBOARD = listOf(
        "Hakeem Olajuwon" to 3830L,
        "Dikembe Mutombo" to 3289L,
        "Kareem Abdul-Jabbar" to 3189L,
        "Artis Gilmore" to 3178L,
        "Mark Eaton" to 3064L,
        "Tim Duncan" to 3020L,
        "David Robinson" to 2954L,
        "Patrick Ewing" to 2894L,
        "Shaquille O'Neal" to 2732L,
        "Tree Rollins" to 2542L
    )

    fun calculateRecordBookCategory(
        categoryName: String,
        userPlayerName: String,
        userStatValue: Long
    ): RecordBookCategory {
        val baseList = when (categoryName.lowercase(Locale.ROOT)) {
            "points" -> POINTS_LEADERBOARD
            "rebounds" -> REBOUNDS_LEADERBOARD
            "assists" -> ASSISTS_LEADERBOARD
            "steals" -> STEALS_LEADERBOARD
            "blocks" -> BLOCKS_LEADERBOARD
            else -> POINTS_LEADERBOARD
        }

        // Combine legends + user
        val userEntry = Pair(userPlayerName, userStatValue)
        val combined = (baseList + userEntry).sortedByDescending { it.second }

        val userRank = combined.indexOfFirst { it.first == userPlayerName && it.second == userStatValue } + 1

        val finalRankings = mutableListOf<NbaLegendRecord>()
        if (userRank <= 10) {
            // User is in top 10
            combined.take(10).forEachIndexed { idx, pair ->
                finalRankings.add(
                    NbaLegendRecord(
                        rank = idx + 1,
                        playerName = pair.first,
                        statValue = pair.second,
                        isUser = pair.first == userPlayerName && pair.second == userStatValue
                    )
                )
            }
        } else {
            // User is outside top 10
            baseList.take(10).forEachIndexed { idx, pair ->
                finalRankings.add(
                    NbaLegendRecord(
                        rank = idx + 1,
                        playerName = pair.first,
                        statValue = pair.second,
                        isUser = false
                    )
                )
            }
            finalRankings.add(
                NbaLegendRecord(
                    rank = userRank,
                    playerName = userPlayerName,
                    statValue = userStatValue,
                    isUser = true
                )
            )
        }

        val nextLegendName: String?
        val statsToNextLegend: Long

        if (userRank == 1) {
            nextLegendName = null
            statsToNextLegend = 0L
        } else {
            val legendAbove = combined[userRank - 2] // index is userRank - 1, so above is userRank - 2
            nextLegendName = legendAbove.first
            statsToNextLegend = (legendAbove.second - userStatValue) + 1L
        }

        return RecordBookCategory(
            categoryName = categoryName,
            userStatValue = userStatValue,
            userRank = userRank,
            rankings = finalRankings,
            nextLegendName = nextLegendName,
            statsToNextLegend = statsToNextLegend
        )
    }
}
