package com.example.playerlock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profiles")
data class PlayerProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerName: String = "MyPlayer",
    val position: String = "PG",
    val overallRating: Int = 60,
    val availableAp: Int = 0,
    val availableBp: Int = 0,
    val xpRemainderForAp: Double = 0.0,
    val xpRemainderForBp: Double = 0.0,
    val avatarPath: String? = null,
    val heightInches: Int = 78,
    val weightLbs: Int = 210,
    val jerseyNumber: Int = 23,
    val teamName: String = "Draft Prospect",
    val createdAt: Long = System.currentTimeMillis(),
    val currentSeason: Int = 1,
    val seasonStartOvr: Int = 60,
    val isInPlayoffs: Boolean = false,
    val playoffBracketJson: String? = null
) {
    val formattedHeight: String
        get() = "${heightInches / 12}'${heightInches % 12}\""

    val formattedVitals: String
        get() = "$formattedHeight • $weightLbs lbs • #$jerseyNumber • $teamName"
}
