package com.example.playerlock.data

data class AppSettings(
    val quarterLengthMinutes: Int = 12,
    val normalizeQuarterXp: Boolean = true,
    val selectedThemeId: String = "2k_gold",
    val themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    val pureBlackAmoled: Boolean = false,
    val personalizePrimaryColor: Long = 0xFFFFB300L,
    val personalizeSecondaryColor: Long = 0xFF10B981L,
    val autoSaveEnabled: Boolean = false,
    val autoSaveFolderUri: String? = null,
    val lastAutoSaveTimestamp: Long = 0L
)
