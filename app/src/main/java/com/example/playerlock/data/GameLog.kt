package com.example.playerlock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_logs")
data class GameLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int = 1,
    val seasonNumber: Int = 1,
    val datePlayed: Long = System.currentTimeMillis(),
    val minutesPlayed: Int = 0,
    val points: Int = 0,
    val rebounds: Int = 0,
    val assists: Int = 0,
    val steals: Int = 0,
    val blocks: Int = 0,
    val turnovers: Int = 0,
    val fieldGoalsAttempted: Int = 0,
    val fieldGoalsMade: Int = 0,
    val freeThrowsAttempted: Int = 0,
    val freeThrowsMade: Int = 0,
    val isWin: Boolean = false,
    val isPlayoff: Boolean = false,
    val teammateGrade: String = "B",
    val quarterMinutes: Int = 12,
    val playerOfTheGame: Boolean = false,
    val playerOfTheWeek: Boolean = false,
    val allStar: Boolean = false,
    val majorAward: Boolean = false
) {
    val xpEarned: Int
        get() {
            val inputs = com.example.playerlock.progression.ProgressionCalculator.GameInputs(
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
                useQuarterNormalizer = true,
                isDoubleDouble = (points >= 10 && rebounds >= 10) || (points >= 10 && assists >= 10) || (rebounds >= 10 && assists >= 10),
                isTripleDouble = points >= 10 && rebounds >= 10 && assists >= 10,
                isPlayerOfTheGame = playerOfTheGame,
                isPlayerOfTheWeek = playerOfTheWeek,
                isAllStar = allStar,
                isMajorAward = majorAward
            )
            val result = com.example.playerlock.progression.ProgressionCalculator.calculateGameXp(inputs, 0.0, 0.0)
            return result.totalXp.toInt()
        }
}
