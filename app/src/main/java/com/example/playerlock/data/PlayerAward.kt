package com.example.playerlock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_awards")
data class PlayerAward(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int = 1,
    val awardName: String,
    val category: String, // "WEEKLY", "ALL_STAR", "ALL_NBA", "ALL_DEFENSE", "MAJOR"
    val seasonNumber: Int = 1,
    val xpGranted: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
