package com.example.playerlock.data

import org.json.JSONArray
import org.json.JSONObject

data class PlayoffSeries(
    val id: String,
    val round: Int, // 1 = R1, 2 = Semis, 3 = Conf Finals, 4 = NBA Finals
    val topTeamAbbr: String,
    val topSeed: Int,
    val topWins: Int = 0,
    val bottomTeamAbbr: String,
    val bottomSeed: Int,
    val bottomWins: Int = 0,
    val isUserSeries: Boolean = false,
    val isFinished: Boolean = false
) {
    val winnerAbbr: String?
        get() = when {
            topWins >= 4 -> topTeamAbbr
            bottomWins >= 4 -> bottomTeamAbbr
            else -> null
        }

    val winnerSeed: Int?
        get() = when {
            topWins >= 4 -> topSeed
            bottomWins >= 4 -> bottomSeed
            else -> null
        }

    val isTbd: Boolean
        get() = topTeamAbbr == "TBD" || bottomTeamAbbr == "TBD" || topTeamAbbr.isBlank() || bottomTeamAbbr.isBlank()
}

data class PlayoffBracketState(
    val userConference: String = "West",
    val userSeed: Int = 1,
    val userTeamAbbr: String = "LAL",
    val currentRound: Int = 1,
    val finalsOpponentAbbr: String = "BOS",
    val round1Matchups: List<PlayoffSeries> = emptyList(),
    val semisMatchups: List<PlayoffSeries> = emptyList(),
    val confFinalsMatchup: PlayoffSeries? = null,
    val nbaFinalsMatchup: PlayoffSeries? = null
) {
    val activeUserSeries: PlayoffSeries?
        get() = when (currentRound) {
            1 -> round1Matchups.firstOrNull { it.isUserSeries || it.topTeamAbbr == userTeamAbbr || it.bottomTeamAbbr == userTeamAbbr }
            2 -> semisMatchups.firstOrNull { it.isUserSeries || it.topTeamAbbr == userTeamAbbr || it.bottomTeamAbbr == userTeamAbbr }
            3 -> if (confFinalsMatchup?.let { it.isUserSeries || it.topTeamAbbr == userTeamAbbr || it.bottomTeamAbbr == userTeamAbbr } == true) confFinalsMatchup else null
            4 -> if (nbaFinalsMatchup?.let { it.isUserSeries || it.topTeamAbbr == userTeamAbbr || it.bottomTeamAbbr == userTeamAbbr } == true) nbaFinalsMatchup else null
            else -> null
        }

    val currentRoundName: String
        get() = when (currentRound) {
            1 -> "First Round"
            2 -> "Conf. Semifinals"
            3 -> "$userConference Conference Finals"
            4 -> "NBA Finals"
            else -> "Playoffs"
        }

    val isCurrentRoundComplete: Boolean
        get() {
            val seriesList = when (currentRound) {
                1 -> round1Matchups
                2 -> semisMatchups
                3 -> listOfNotNull(confFinalsMatchup)
                4 -> listOfNotNull(nbaFinalsMatchup)
                else -> emptyList()
            }
            return seriesList.isNotEmpty() && seriesList.all { it.isFinished || it.topWins >= 4 || it.bottomWins >= 4 }
        }

    fun toJson(): String {
        val root = JSONObject()
        root.put("userConference", userConference)
        root.put("userSeed", userSeed)
        root.put("userTeamAbbr", userTeamAbbr)
        root.put("currentRound", currentRound)
        root.put("finalsOpponentAbbr", finalsOpponentAbbr)

        fun seriesToJson(series: PlayoffSeries): JSONObject {
            return JSONObject().apply {
                put("id", series.id)
                put("round", series.round)
                put("topTeamAbbr", series.topTeamAbbr)
                put("topSeed", series.topSeed)
                put("topWins", series.topWins)
                put("bottomTeamAbbr", series.bottomTeamAbbr)
                put("bottomSeed", series.bottomSeed)
                put("bottomWins", series.bottomWins)
                put("isUserSeries", series.isUserSeries)
                put("isFinished", series.isFinished)
            }
        }

        val r1Array = JSONArray()
        round1Matchups.forEach { r1Array.put(seriesToJson(it)) }
        root.put("round1Matchups", r1Array)

        val semisArray = JSONArray()
        semisMatchups.forEach { semisArray.put(seriesToJson(it)) }
        root.put("semisMatchups", semisArray)

        confFinalsMatchup?.let { root.put("confFinalsMatchup", seriesToJson(it)) }
        nbaFinalsMatchup?.let { root.put("nbaFinalsMatchup", seriesToJson(it)) }

        return root.toString()
    }

    companion object {
        fun fromJson(jsonStr: String?): PlayoffBracketState? {
            if (jsonStr.isNullOrBlank()) return null
            return try {
                val root = JSONObject(jsonStr)

                fun seriesFromJson(obj: JSONObject?): PlayoffSeries? {
                    if (obj == null) return null
                    return PlayoffSeries(
                        id = obj.getString("id"),
                        round = obj.getInt("round"),
                        topTeamAbbr = obj.getString("topTeamAbbr"),
                        topSeed = obj.getInt("topSeed"),
                        topWins = obj.optInt("topWins", 0),
                        bottomTeamAbbr = obj.getString("bottomTeamAbbr"),
                        bottomSeed = obj.getInt("bottomSeed"),
                        bottomWins = obj.optInt("bottomWins", 0),
                        isUserSeries = obj.optBoolean("isUserSeries", false),
                        isFinished = obj.optBoolean("isFinished", false)
                    )
                }

                val r1List = mutableListOf<PlayoffSeries>()
                val r1Array = root.optJSONArray("round1Matchups")
                if (r1Array != null) {
                    for (i in 0 until r1Array.length()) {
                        seriesFromJson(r1Array.getJSONObject(i))?.let { r1List.add(it) }
                    }
                }

                val semisList = mutableListOf<PlayoffSeries>()
                val semisArray = root.optJSONArray("semisMatchups")
                if (semisArray != null) {
                    for (i in 0 until semisArray.length()) {
                        seriesFromJson(semisArray.getJSONObject(i))?.let { semisList.add(it) }
                    }
                }

                val rawState = PlayoffBracketState(
                    userConference = root.optString("userConference", "West"),
                    userSeed = root.optInt("userSeed", 1),
                    userTeamAbbr = root.optString("userTeamAbbr", "LAL"),
                    currentRound = root.optInt("currentRound", 1),
                    finalsOpponentAbbr = root.optString("finalsOpponentAbbr", "BOS"),
                    round1Matchups = r1List,
                    semisMatchups = semisList,
                    confFinalsMatchup = seriesFromJson(root.optJSONObject("confFinalsMatchup")),
                    nbaFinalsMatchup = seriesFromJson(root.optJSONObject("nbaFinalsMatchup"))
                )

                refreshBracket(rawState)
            } catch (_: Exception) {
                null
            }
        }

        fun createInitialBracket(
            userConference: String,
            userSeed: Int,
            userTeamAbbr: String,
            slotAbbrs: List<String>,
            finalsOpponentAbbr: String
        ): PlayoffBracketState {
            fun abbr(seed: Int): String = slotAbbrs.getOrNull(seed - 1) ?: ""

            val r1 = listOf(
                Triple(1, 8, "r1_1v8"),
                Triple(4, 5, "r1_4v5"),
                Triple(3, 6, "r1_3v6"),
                Triple(2, 7, "r1_2v7")
            ).map { (top, bot, id) ->
                val topAbbr = abbr(top)
                val botAbbr = abbr(bot)
                PlayoffSeries(
                    id = id,
                    round = 1,
                    topTeamAbbr = topAbbr,
                    topSeed = top,
                    topWins = 0,
                    bottomTeamAbbr = botAbbr,
                    bottomSeed = bot,
                    bottomWins = 0,
                    isUserSeries = (top == userSeed || bot == userSeed || topAbbr == userTeamAbbr || botAbbr == userTeamAbbr),
                    isFinished = false
                )
            }

            val semis = listOf(
                PlayoffSeries("semi_1", 2, "TBD", 1, 0, "TBD", 4, 0, isUserSeries = false, isFinished = false),
                PlayoffSeries("semi_2", 2, "TBD", 3, 0, "TBD", 2, 0, isUserSeries = false, isFinished = false)
            )

            val confFinals = PlayoffSeries("conf_finals", 3, "TBD", 1, 0, "TBD", 2, 0, isUserSeries = false, isFinished = false)

            val nbaFinals = PlayoffSeries("nba_finals", 4, "TBD", 1, 0, finalsOpponentAbbr, 1, 0, isUserSeries = true, isFinished = false)

            val base = PlayoffBracketState(
                userConference = userConference,
                userSeed = userSeed,
                userTeamAbbr = userTeamAbbr,
                currentRound = 1,
                finalsOpponentAbbr = finalsOpponentAbbr,
                round1Matchups = r1,
                semisMatchups = semis,
                confFinalsMatchup = confFinals,
                nbaFinalsMatchup = nbaFinals
            )

            return refreshBracket(base)
        }

        fun refreshBracket(bracket: PlayoffBracketState): PlayoffBracketState {
            val userSeed = bracket.userSeed
            val userTeamAbbr = bracket.userTeamAbbr

            val r1 = bracket.round1Matchups
            val w0 = r1.getOrNull(0)?.winnerAbbr
            val s0 = r1.getOrNull(0)?.winnerSeed ?: 1
            val w1 = r1.getOrNull(1)?.winnerAbbr
            val s1 = r1.getOrNull(1)?.winnerSeed ?: 4
            val w2 = r1.getOrNull(2)?.winnerAbbr
            val s2 = r1.getOrNull(2)?.winnerSeed ?: 3
            val w3 = r1.getOrNull(3)?.winnerAbbr
            val s3 = r1.getOrNull(3)?.winnerSeed ?: 2

            val s1Existing = bracket.semisMatchups.getOrNull(0) ?: PlayoffSeries("semi_1", 2, "TBD", 1, 0, "TBD", 4, 0)
            val s2Existing = bracket.semisMatchups.getOrNull(1) ?: PlayoffSeries("semi_2", 2, "TBD", 3, 0, "TBD", 2, 0)

            val top1Abbr = w0 ?: s1Existing.topTeamAbbr
            val bot1Abbr = w1 ?: s1Existing.bottomTeamAbbr
            val semi1 = s1Existing.copy(
                topTeamAbbr = top1Abbr,
                topSeed = if (w0 != null) s0 else s1Existing.topSeed,
                bottomTeamAbbr = bot1Abbr,
                bottomSeed = if (w1 != null) s1 else s1Existing.bottomSeed,
                isUserSeries = (top1Abbr == userTeamAbbr || bot1Abbr == userTeamAbbr || (s0 == userSeed && w0 != null) || (s1 == userSeed && w1 != null))
            )

            val top2Abbr = w2 ?: s2Existing.topTeamAbbr
            val bot2Abbr = w3 ?: s2Existing.bottomTeamAbbr
            val semi2 = s2Existing.copy(
                topTeamAbbr = top2Abbr,
                topSeed = if (w2 != null) s2 else s2Existing.topSeed,
                bottomTeamAbbr = bot2Abbr,
                bottomSeed = if (w3 != null) s3 else s2Existing.bottomSeed,
                isUserSeries = (top2Abbr == userTeamAbbr || bot2Abbr == userTeamAbbr || (s2 == userSeed && w2 != null) || (s3 == userSeed && w3 != null))
            )

            val semis = listOf(semi1, semi2)

            val cfExisting = bracket.confFinalsMatchup ?: PlayoffSeries("conf_finals", 3, "TBD", 1, 0, "TBD", 2, 0)
            val sw0 = semi1.winnerAbbr
            val ss0 = semi1.winnerSeed ?: 1
            val sw1 = semi2.winnerAbbr
            val ss1 = semi2.winnerSeed ?: 2

            val cfTopAbbr = sw0 ?: cfExisting.topTeamAbbr
            val cfBotAbbr = sw1 ?: cfExisting.bottomTeamAbbr
            val confFinals = cfExisting.copy(
                topTeamAbbr = cfTopAbbr,
                topSeed = if (sw0 != null) ss0 else cfExisting.topSeed,
                bottomTeamAbbr = cfBotAbbr,
                bottomSeed = if (sw1 != null) ss1 else cfExisting.bottomSeed,
                isUserSeries = (cfTopAbbr == userTeamAbbr || cfBotAbbr == userTeamAbbr)
            )

            val nbaExisting = bracket.nbaFinalsMatchup ?: PlayoffSeries("nba_finals", 4, "TBD", 1, 0, bracket.finalsOpponentAbbr, 1, 0)
            val cfWinner = confFinals.winnerAbbr
            val cfWinnerSeed = confFinals.winnerSeed ?: 1

            val nbaTopAbbr = cfWinner ?: nbaExisting.topTeamAbbr
            val nbaFinals = nbaExisting.copy(
                topTeamAbbr = nbaTopAbbr,
                topSeed = if (cfWinner != null) cfWinnerSeed else nbaExisting.topSeed,
                bottomTeamAbbr = bracket.finalsOpponentAbbr,
                bottomSeed = 1,
                isUserSeries = (nbaTopAbbr == userTeamAbbr)
            )

            var nextRound = bracket.currentRound
            val r1Finished = r1.size >= 4 && r1.all { it.isFinished || it.topWins >= 4 || it.bottomWins >= 4 }
            val semisFinished = semis.size >= 2 && semis.all { it.isFinished || it.topWins >= 4 || it.bottomWins >= 4 }
            val cfFinished = confFinals.isFinished || confFinals.topWins >= 4 || confFinals.bottomWins >= 4

            if (nextRound == 1 && r1Finished) {
                nextRound = 2
            }
            if (nextRound == 2 && semisFinished) {
                nextRound = 3
            }
            if (nextRound == 3 && cfFinished) {
                nextRound = 4
            }

            return bracket.copy(
                currentRound = nextRound,
                semisMatchups = semis,
                confFinalsMatchup = confFinals,
                nbaFinalsMatchup = nbaFinals
            )
        }
    }
}
