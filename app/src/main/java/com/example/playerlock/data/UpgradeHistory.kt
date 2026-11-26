package com.example.playerlock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upgrade_history")
data class UpgradeHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int = 1,
    val seasonNumber: Int = 1,
    val entityType: String,
    val entityId: Int,
    val oldValue: Int,
    val newValue: Int,
    val costSpent: Int,
    val currencyType: String,
    val isAppliedIn2k: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
