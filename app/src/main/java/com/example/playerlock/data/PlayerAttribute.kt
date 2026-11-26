package com.example.playerlock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_attributes")
data class PlayerAttribute(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int = 1,
    val category: String,
    val attributeName: String,
    val sortOrder: Int = 0,
    val currentValue: Int,
    val maxValue: Int = 99
)
