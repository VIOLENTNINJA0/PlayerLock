package com.example.playerlock.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.playerlock.data.NbaTeamCatalog

data class AppThemePreset(
    val id: String,
    val name: String,
    val category: String,
    val primaryLight: Color,
    val secondaryLight: Color,
    val primaryDark: Color,
    val secondaryDark: Color
)

object AppThemeCatalog {

    val FEATURED_PRESETS: List<AppThemePreset> = listOf(
        AppThemePreset(
            id = "2k_gold",
            name = "2K Broadcast Gold",
            category = "Featured",
            primaryLight = Color(0xFFFFB300),
            secondaryLight = Color(0xFF10B981),
            primaryDark = Color(0xFFFFC107),
            secondaryDark = Color(0xFF00C853)
        ),
        AppThemePreset(
            id = "cobalt_blue",
            name = "Cobalt Blue",
            category = "Featured",
            primaryLight = Color(0xFF2979FF),
            secondaryLight = Color(0xFF00E5FF),
            primaryDark = Color(0xFF448AFF),
            secondaryDark = Color(0xFF18FFFF)
        ),
        AppThemePreset(
            id = "royal_lavender",
            name = "Royal Lavender",
            category = "Featured",
            primaryLight = Color(0xFF7C4DFF),
            secondaryLight = Color(0xFFFF80AB),
            primaryDark = Color(0xFFB388FF),
            secondaryDark = Color(0xFFFF80AB)
        ),
        AppThemePreset(
            id = "crimson_rose",
            name = "Crimson Rose",
            category = "Featured",
            primaryLight = Color(0xFFFF4081),
            secondaryLight = Color(0xFFFFAB91),
            primaryDark = Color(0xFFFF4081),
            secondaryDark = Color(0xFFFFAB91)
        ),
        AppThemePreset(
            id = "emerald_mint",
            name = "Emerald Mint",
            category = "Featured",
            primaryLight = Color(0xFF00E676),
            secondaryLight = Color(0xFF1DE9B6),
            primaryDark = Color(0xFF00E676),
            secondaryDark = Color(0xFF1DE9B6)
        ),
        AppThemePreset(
            id = "midnight_indigo",
            name = "Midnight Indigo",
            category = "Featured",
            primaryLight = Color(0xFF651FFF),
            secondaryLight = Color(0xFFFFD740),
            primaryDark = Color(0xFF7C4DFF),
            secondaryDark = Color(0xFFFFD740)
        ),
        AppThemePreset(
            id = "solar_citrus",
            name = "Solar Citrus",
            category = "Featured",
            primaryLight = Color(0xFFFF6D00),
            secondaryLight = Color(0xFF64DD17),
            primaryDark = Color(0xFFFFAB40),
            secondaryDark = Color(0xFF76FF03)
        )
    )

    fun getThemePresets(
        activeTeamName: String?,
        customPrimary: Color,
        customSecondary: Color,
        categoryFilter: String = "All"
    ): List<AppThemePreset> {
        val list = mutableListOf<AppThemePreset>()

        // 1. Featured Presets
        list.addAll(FEATURED_PRESETS)

        // 2. Dynamic Active Player's Team Preset
        val activeScheme = NbaTeamCatalog.getScheme(activeTeamName ?: "Draft Prospect")
        val activeTeamPreset = AppThemePreset(
            id = "active_team",
            name = "Active Player's Team",
            category = "Featured",
            primaryLight = adjustContrast(activeScheme.primaryColor, isDark = false),
            secondaryLight = adjustContrast(activeScheme.secondaryColor, isDark = false),
            primaryDark = adjustContrast(activeScheme.primaryColor, isDark = true),
            secondaryDark = adjustContrast(activeScheme.secondaryColor, isDark = true)
        )
        list.add(activeTeamPreset)

        // 3. All 30 NBA Teams
        val nbaPresets = NbaTeamCatalog.TEAMS.filter { it.abbreviation != "PRO" }.map { team ->
            AppThemePreset(
                id = "nba_${team.abbreviation.lowercase()}",
                name = team.name,
                category = "NBA Teams",
                primaryLight = adjustContrast(team.primaryColor, isDark = false),
                secondaryLight = adjustContrast(team.secondaryColor, isDark = false),
                primaryDark = adjustContrast(team.primaryColor, isDark = true),
                secondaryDark = adjustContrast(team.secondaryColor, isDark = true)
            )
        }
        list.addAll(nbaPresets)

        // Filter by Category
        val filtered = when (categoryFilter) {
            "Featured" -> list.filter { it.category == "Featured" }
            "NBA Teams" -> list.filter { it.category == "NBA Teams" }
            else -> list // "All"
        }.toMutableList()

        // 4. Personalize Preset (ALWAYS PLACED LAST)
        val personalizePreset = AppThemePreset(
            id = "personalize",
            name = "Personalize",
            category = "Featured",
            primaryLight = customPrimary,
            secondaryLight = customSecondary,
            primaryDark = customPrimary,
            secondaryDark = customSecondary
        )
        filtered.removeAll { it.id == "personalize" }
        filtered.add(personalizePreset)

        return filtered
    }

    fun getPresetById(
        id: String,
        activeTeamName: String?,
        customPrimary: Color,
        customSecondary: Color
    ): AppThemePreset {
        val all = getThemePresets(activeTeamName, customPrimary, customSecondary, "All")
        return all.firstOrNull { it.id == id } ?: FEATURED_PRESETS.first()
    }

    /**
     * Intentionally a pass-through. Contrast is handled once, in Theme.kt, by changing
     * brightness only, so team colours keep their real hue and saturation.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun adjustContrast(color: Color, isDark: Boolean): Color = color
}
