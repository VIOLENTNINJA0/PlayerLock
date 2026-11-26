package com.example.playerlock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_badges")
data class PlayerBadge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int = 1,
    val badgeName: String,
    val tier: String = "None",
    val equipped: Boolean = false,
    val category: String = "Other",
    val sortOrder: Int = 0,
    val description: String = ""
)
