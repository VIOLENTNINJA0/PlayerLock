package com.example.playerlock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "season_summaries")
data class SeasonSummary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int = 1,
    val seasonNumber: Int = 1,
    val wins: Int = 0,
    val losses: Int = 0,
    val gamesPlayed: Int = 0,
    val mpg: Double = 0.0,
    val totalMinutes: Int = 0,
    val ppg: Double = 0.0,
    val rpg: Double = 0.0,
    val apg: Double = 0.0,
    val spg: Double = 0.0,
    val bpg: Double = 0.0,
    val fgPct: Double = 0.0,
    val ftPct: Double = 0.0,
    val doubleDoubles: Int = 0,
    val tripleDoubles: Int = 0,
    val seasonHighSummary: String = "",
    val startOvr: Int = 60,
    val endOvr: Int = 60,
    val attributeUpgradesSummary: String = "",
    val badgeUpgradesSummary: String = "",
    val awardsSummary: String = "",
    val totalXpEarned: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    // Playoff result string, e.g. "NBA Champions (16-2)" or "Eliminated in Conf. Finals (10-6)"
    val playoffResult: String = ""
)
