package com.example.playerlock.data

import androidx.compose.ui.graphics.Color

data class NbaTeamScheme(
    val name: String,
    val abbreviation: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val numberColor: Color,
    val conference: String = "West"
)

object NbaTeamCatalog {

    val TEAMS: List<NbaTeamScheme> = listOf(
        NbaTeamScheme("Draft Prospect", "PRO", Color(0xFF1E1E1E), Color(0xFFFFB300), Color(0xFFFFFFFF), "West"),
        NbaTeamScheme("Los Angeles Lakers", "LAL", Color(0xFF552583), Color(0xFFFDB927), Color(0xFFFFFFFF), "West"),
        NbaTeamScheme("Boston Celtics", "BOS", Color(0xFF007A33), Color(0xFFFFFFFF), Color(0xFFFFFFFF), "East"),
        NbaTeamScheme("Chicago Bulls", "CHI", Color(0xFFCE1141), Color(0xFF000000), Color(0xFFFFFFFF), "East"),
        NbaTeamScheme("Golden State Warriors", "GSW", Color(0xFF1D428A), Color(0xFFFFC72C), Color(0xFFFFFFFF), "West"),
        NbaTeamScheme("Miami Heat", "MIA", Color(0xFF98002E), Color(0xFFF9A01B), Color(0xFFFFFFFF), "East"),
        NbaTeamScheme("New York Knicks", "NYK", Color(0xFF006BB6), Color(0xFFF58426), Color(0xFFFFFFFF), "East"),
        NbaTeamScheme("Brooklyn Nets", "BKN", Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFFFFFFFF), "East"),
        NbaTeamScheme("Milwaukee Bucks", "MIL", Color(0xFF00471B), Color(0xFFEEE1C6), Color(0xFFFFFFFF), "East"),
        NbaTeamScheme("Philadelphia 76ers", "PHI", Color(0xFF006BB6), Color(0xFFED174C), Color(0xFFFFFFFF), "East"),
        NbaTeamScheme("Dallas Mavericks", "DAL", Color(0xFF00538C), Color(0xFFB8C4CA), Color(0xFFFFFFFF), "West"),
        NbaTeamScheme("Denver Nuggets", "DEN", Color(0xFF0E2240), Color(0xFFFEC524), Color(0xFFFFFFFF), "West"),
        NbaTeamScheme("Phoenix Suns", "PHX", Color(0xFF1D1160), Color(0xFFE56020), Color(0xFFFFFFFF), "West"),
        NbaTeamScheme("LA Clippers", "LAC", Color(0xFFC8102E), Color(0xFF1D428A), Color(0xFFFFFFFF), "West"),
        NbaTeamScheme("San Antonio Spurs", "SAS", Color(0xFF000000), Color(0xFFC4CED4), Color(0xFFFFFFFF), "West"),
        NbaTeamScheme("Houston Rockets", "HOU", Color(0xFFCE1141), Color(0xFF000000), Color(0xFFFFFFFF), "West"),
        NbaTeamScheme("Memphis Grizzlies", "MEM", Color(0xFF5D76A9), Color(0xFF12173F), Color(0xFFFFD100), "West"),
        NbaTeamScheme("New Orleans Pelicans", "NOP", Color(0xFF0C2340), Color(0xFFC8102E), Color(0xFFB4975A), "West"),
        NbaTeamScheme("Oklahoma City Thunder", "OKC", Color(0xFF007AC1), Color(0xFFEF3B24), Color(0xFFFFFFFF), "West"),
        NbaTeamScheme("Minnesota Timberwolves", "MIN", Color(0xFF0C2340), Color(0xFF78BE20), Color(0xFFFFFFFF), "West"),
        NbaTeamScheme("Portland Trail Blazers", "POR", Color(0xFFE03A3E), Color(0xFF000000), Color(0xFFFFFFFF), "West"),
        NbaTeamScheme("Utah Jazz", "UTA", Color(0xFF002B5C), Color(0xFFF9A01B), Color(0xFFFFFFFF), "West"),
        NbaTeamScheme("Sacramento Kings", "SAC", Color(0xFF5A2D81), Color(0xFF63727A), Color(0xFFFFFFFF), "West"),
        NbaTeamScheme("Atlanta Hawks", "ATL", Color(0xFFC8102E), Color(0xFFFDB927), Color(0xFFFFFFFF), "East"),
        NbaTeamScheme("Charlotte Hornets", "CHA", Color(0xFF1D1160), Color(0xFF00788C), Color(0xFFFFFFFF), "East"),
        NbaTeamScheme("Cleveland Cavaliers", "CLE", Color(0xFF860038), Color(0xFFFDBB30), Color(0xFFFFFFFF), "East"),
        NbaTeamScheme("Detroit Pistons", "DET", Color(0xFFC8102E), Color(0xFF1D428A), Color(0xFFFFFFFF), "East"),
        NbaTeamScheme("Indiana Pacers", "IND", Color(0xFF002D62), Color(0xFFFDBB30), Color(0xFFFFFFFF), "East"),
        NbaTeamScheme("Orlando Magic", "ORL", Color(0xFF0077C0), Color(0xFFC4CED4), Color(0xFF000000), "East"),
        NbaTeamScheme("Toronto Raptors", "TOR", Color(0xFFCE1141), Color(0xFF000000), Color(0xFFFFFFFF), "East"),
        NbaTeamScheme("Washington Wizards", "WAS", Color(0xFF002B5C), Color(0xFFE31837), Color(0xFFFFFFFF), "East")
    )

    fun getScheme(teamName: String): NbaTeamScheme {
        return TEAMS.firstOrNull {
            it.name.equals(teamName, ignoreCase = true) ||
                    it.abbreviation.equals(teamName, ignoreCase = true)
        } ?: TEAMS.first()
    }

    fun getTeamsForConference(conf: String): List<NbaTeamScheme> {
        return TEAMS.filter { it.conference.equals(conf, ignoreCase = true) && it.abbreviation != "PRO" }
    }

    fun getSchemeByAbbr(abbr: String): NbaTeamScheme {
        return TEAMS.firstOrNull { it.abbreviation.equals(abbr, ignoreCase = true) } ?: TEAMS.first()
    }
}
