package com.example.playerlock.data

object AttributeSeedData {

    val ATTRIBUTES: List<PlayerAttribute> = listOf(
        // Offense (1..18)
        PlayerAttribute(id = 1, category = "Offense", attributeName = "Driving Layup", sortOrder = 1, currentValue = 60),
        PlayerAttribute(id = 2, category = "Offense", attributeName = "Post Fade", sortOrder = 2, currentValue = 60),
        PlayerAttribute(id = 3, category = "Offense", attributeName = "Post Hook", sortOrder = 3, currentValue = 60),
        PlayerAttribute(id = 4, category = "Offense", attributeName = "Post Control", sortOrder = 4, currentValue = 60),
        PlayerAttribute(id = 5, category = "Offense", attributeName = "Draw Foul", sortOrder = 5, currentValue = 60),
        PlayerAttribute(id = 6, category = "Offense", attributeName = "Close Shot", sortOrder = 6, currentValue = 60),
        PlayerAttribute(id = 7, category = "Offense", attributeName = "Mid-Range Shot", sortOrder = 7, currentValue = 60),
        PlayerAttribute(id = 8, category = "Offense", attributeName = "Three-Point Shot", sortOrder = 8, currentValue = 60),
        PlayerAttribute(id = 9, category = "Offense", attributeName = "Free Throw", sortOrder = 9, currentValue = 60),
        PlayerAttribute(id = 10, category = "Offense", attributeName = "Ball Handle", sortOrder = 10, currentValue = 60),
        PlayerAttribute(id = 11, category = "Offense", attributeName = "Pass IQ", sortOrder = 11, currentValue = 60),
        PlayerAttribute(id = 12, category = "Offense", attributeName = "Pass Accuracy", sortOrder = 12, currentValue = 60),
        PlayerAttribute(id = 13, category = "Offense", attributeName = "Offensive Rebound", sortOrder = 13, currentValue = 60),
        PlayerAttribute(id = 14, category = "Offense", attributeName = "Standing Dunk", sortOrder = 14, currentValue = 60),
        PlayerAttribute(id = 15, category = "Offense", attributeName = "Driving Dunk", sortOrder = 15, currentValue = 60),
        PlayerAttribute(id = 16, category = "Offense", attributeName = "Shot IQ", sortOrder = 16, currentValue = 60),
        PlayerAttribute(id = 17, category = "Offense", attributeName = "Pass Vision", sortOrder = 17, currentValue = 60),
        PlayerAttribute(id = 18, category = "Offense", attributeName = "Hands", sortOrder = 18, currentValue = 60),
        // Defense (19..23)
        PlayerAttribute(id = 19, category = "Defense", attributeName = "Defensive Rebound", sortOrder = 19, currentValue = 60),
        PlayerAttribute(id = 20, category = "Defense", attributeName = "Interior Defense", sortOrder = 20, currentValue = 60),
        PlayerAttribute(id = 21, category = "Defense", attributeName = "Perimeter Defense", sortOrder = 21, currentValue = 60),
        PlayerAttribute(id = 22, category = "Defense", attributeName = "Block", sortOrder = 22, currentValue = 60),
        PlayerAttribute(id = 23, category = "Defense", attributeName = "Steal", sortOrder = 23, currentValue = 60),
        // Athleticism (24..30)
        PlayerAttribute(id = 24, category = "Athleticism", attributeName = "Speed", sortOrder = 24, currentValue = 60),
        PlayerAttribute(id = 25, category = "Athleticism", attributeName = "Speed with Ball", sortOrder = 25, currentValue = 60),
        PlayerAttribute(id = 26, category = "Athleticism", attributeName = "Vertical", sortOrder = 26, currentValue = 60),
        PlayerAttribute(id = 27, category = "Athleticism", attributeName = "Strength", sortOrder = 27, currentValue = 60),
        PlayerAttribute(id = 28, category = "Athleticism", attributeName = "Stamina", sortOrder = 28, currentValue = 60),
        PlayerAttribute(id = 29, category = "Athleticism", attributeName = "Hustle", sortOrder = 29, currentValue = 60),
        PlayerAttribute(id = 30, category = "Athleticism", attributeName = "Agility", sortOrder = 30, currentValue = 60),
        // Mental (31..34)
        PlayerAttribute(id = 31, category = "Mental", attributeName = "Pass Perception", sortOrder = 31, currentValue = 60),
        PlayerAttribute(id = 32, category = "Mental", attributeName = "Defensive Consistency", sortOrder = 32, currentValue = 60),
        PlayerAttribute(id = 33, category = "Mental", attributeName = "Help Defense IQ", sortOrder = 33, currentValue = 60),
        PlayerAttribute(id = 34, category = "Mental", attributeName = "Offensive Consistency", sortOrder = 34, currentValue = 60),
        // Misc (35..36)
        PlayerAttribute(id = 35, category = "Misc", attributeName = "Intangibles", sortOrder = 35, currentValue = 60),
        PlayerAttribute(id = 36, category = "Misc", attributeName = "Potential", sortOrder = 36, currentValue = 60)
    )

    private val ARCHETYPE_DELTAS: Map<String, Map<String, Int>> = mapOf(
        "Sharpshooter" to mapOf(
            "Three-Point Shot" to 18,
            "Mid-Range Shot" to 16,
            "Free Throw" to 14,
            "Shot IQ" to 15,
            "Offensive Consistency" to 13,
            "Close Shot" to 4,
            "Pass Accuracy" to 3,
            "Ball Handle" to 4,
            "Perimeter Defense" to 2,
            "Speed with Ball" to 1,
            "Hands" to 5,
            "Pass IQ" to 4,
            "Draw Foul" to 3,
            "Pass Perception" to 3,
            "Hustle" to 3,
            "Speed" to 1,
            "Agility" to 1,
            "Steal" to 1,
            "Help Defense IQ" to 1,
            "Defensive Consistency" to 1,
            "Driving Layup" to 0,
            "Driving Dunk" to -6,
            "Defensive Rebound" to -4,
            "Vertical" to -2,
            "Post Fade" to -4,
            "Strength" to -6,
            "Interior Defense" to -8,
            "Standing Dunk" to -14,
            "Post Hook" to -12,
            "Post Control" to -10,
            "Block" to -10,
            "Offensive Rebound" to -12
        ),
        "Slashing Playmaker" to mapOf(
            "Driving Layup" to 16,
            "Driving Dunk" to 15,
            "Ball Handle" to 18,
            "Speed with Ball" to 16,
            "Pass Accuracy" to 15,
            "Pass Vision" to 14,
            "Speed" to 15,
            "Agility" to 14,
            "Vertical" to 13,
            "Pass IQ" to 12,
            "Close Shot" to 6,
            "Draw Foul" to 7,
            "Hands" to 8,
            "Hustle" to 6,
            "Perimeter Defense" to 4,
            "Steal" to 5,
            "Shot IQ" to 3,
            "Offensive Consistency" to 6,
            "Mid-Range Shot" to 1,
            "Free Throw" to 2,
            "Three-Point Shot" to -2,
            "Defensive Consistency" to 2,
            "Pass Perception" to 3,
            "Help Defense IQ" to 1,
            "Strength" to -4,
            "Defensive Rebound" to -5,
            "Standing Dunk" to -6,
            "Post Control" to -6,
            "Post Hook" to -12,
            "Post Fade" to -10,
            "Interior Defense" to -8,
            "Block" to -8,
            "Offensive Rebound" to -10
        ),
        "Two-Way Wing" to mapOf(
            "Perimeter Defense" to 16,
            "Steal" to 14,
            "Help Defense IQ" to 14,
            "Pass Perception" to 14,
            "Defensive Consistency" to 15,
            "Three-Point Shot" to 12,
            "Speed" to 12,
            "Agility" to 12,
            "Hustle" to 14,
            "Mid-Range Shot" to 6,
            "Shot IQ" to 6,
            "Hands" to 6,
            "Driving Layup" to 5,
            "Driving Dunk" to 5,
            "Defensive Rebound" to 5,
            "Interior Defense" to 5,
            "Block" to 5,
            "Free Throw" to 4,
            "Pass Accuracy" to 3,
            "Ball Handle" to 3,
            "Strength" to 5,
            "Vertical" to 6,
            "Offensive Consistency" to 5,
            "Draw Foul" to 2,
            "Close Shot" to 3,
            "Pass IQ" to 3,
            "Speed with Ball" to 1,
            "Pass Vision" to 0,
            "Offensive Rebound" to -2,
            "Standing Dunk" to -8,
            "Post Control" to -10,
            "Post Fade" to -10,
            "Post Hook" to -12
        ),
        "Shot Creator" to mapOf(
            "Mid-Range Shot" to 18,
            "Close Shot" to 14,
            "Ball Handle" to 15,
            "Speed with Ball" to 14,
            "Driving Layup" to 14,
            "Shot IQ" to 15,
            "Three-Point Shot" to 13,
            "Offensive Consistency" to 14,
            "Free Throw" to 10,
            "Post Fade" to 8,
            "Draw Foul" to 7,
            "Speed" to 8,
            "Agility" to 8,
            "Hands" to 7,
            "Pass Accuracy" to 5,
            "Pass IQ" to 5,
            "Driving Dunk" to 4,
            "Vertical" to 4,
            "Pass Vision" to 3,
            "Hustle" to 4,
            "Perimeter Defense" to 2,
            "Steal" to 2,
            "Pass Perception" to 2,
            "Help Defense IQ" to 0,
            "Defensive Consistency" to 1,
            "Post Control" to -4,
            "Strength" to -4,
            "Defensive Rebound" to -6,
            "Interior Defense" to -8,
            "Block" to -10,
            "Post Hook" to -12,
            "Offensive Rebound" to -12,
            "Standing Dunk" to -14
        ),
        "Paint Beast" to mapOf(
            "Standing Dunk" to 18,
            "Close Shot" to 15,
            "Post Control" to 16,
            "Post Hook" to 16,
            "Interior Defense" to 16,
            "Block" to 16,
            "Offensive Rebound" to 16,
            "Defensive Rebound" to 16,
            "Strength" to 16,
            "Vertical" to 12,
            "Driving Dunk" to 8,
            "Post Fade" to 6,
            "Draw Foul" to 8,
            "Hands" to 8,
            "Help Defense IQ" to 10,
            "Defensive Consistency" to 10,
            "Hustle" to 10,
            "Driving Layup" to 4,
            "Shot IQ" to 3,
            "Pass IQ" to 2,
            "Offensive Consistency" to 6,
            "Pass Accuracy" to -2,
            "Pass Vision" to -4,
            "Speed" to -4,
            "Agility" to -4,
            "Steal" to -6,
            "Pass Perception" to -4,
            "Perimeter Defense" to -8,
            "Free Throw" to -10,
            "Ball Handle" to -14,
            "Speed with Ball" to -14,
            "Mid-Range Shot" to -14,
            "Three-Point Shot" to -16
        ),
        "Balanced" to mapOf(
            "Shot IQ" to 4,
            "Pass IQ" to 4,
            "Help Defense IQ" to 4,
            "Pass Perception" to 3,
            "Defensive Consistency" to 3,
            "Offensive Consistency" to 3,
            "Driving Layup" to 3,
            "Close Shot" to 2,
            "Mid-Range Shot" to 2,
            "Three-Point Shot" to 1,
            "Free Throw" to 2,
            "Hands" to 4,
            "Draw Foul" to 2,
            "Pass Accuracy" to 3,
            "Ball Handle" to 2,
            "Speed with Ball" to 1,
            "Pass Vision" to 1,
            "Perimeter Defense" to 2,
            "Steal" to 2,
            "Interior Defense" to 1,
            "Block" to 1,
            "Driving Dunk" to 2,
            "Standing Dunk" to 0,
            "Defensive Rebound" to 2,
            "Offensive Rebound" to 0,
            "Speed" to 3,
            "Agility" to 3,
            "Strength" to 2,
            "Vertical" to 2,
            "Hustle" to 4,
            "Post Fade" to -1,
            "Post Control" to 0,
            "Post Hook" to -2
        )
    )

    private val POSITION_ADJUSTMENTS: Map<String, Map<String, Int>> = mapOf(
        "PG" to mapOf(
            "Ball Handle" to 3,
            "Speed with Ball" to 3,
            "Pass Accuracy" to 3,
            "Pass Vision" to 3,
            "Speed" to 2,
            "Agility" to 2,
            "Perimeter Defense" to 1,
            "Steal" to 1,
            "Interior Defense" to -3,
            "Block" to -4,
            "Standing Dunk" to -6,
            "Offensive Rebound" to -4,
            "Defensive Rebound" to -3,
            "Strength" to -3
        ),
        "SG" to mapOf(
            "Mid-Range Shot" to 2,
            "Three-Point Shot" to 2,
            "Ball Handle" to 1,
            "Speed with Ball" to 1,
            "Perimeter Defense" to 2,
            "Steal" to 1,
            "Speed" to 1,
            "Agility" to 1,
            "Interior Defense" to -2,
            "Block" to -2,
            "Standing Dunk" to -4,
            "Offensive Rebound" to -3,
            "Strength" to -2
        ),
        "SF" to mapOf(
            "Perimeter Defense" to 2,
            "Driving Dunk" to 2,
            "Defensive Rebound" to 1,
            "Speed" to 1,
            "Agility" to 1,
            "Hustle" to 1
        ),
        "PF" to mapOf(
            "Interior Defense" to 3,
            "Block" to 3,
            "Standing Dunk" to 4,
            "Offensive Rebound" to 4,
            "Defensive Rebound" to 4,
            "Strength" to 3,
            "Post Control" to 2,
            "Post Hook" to 2,
            "Ball Handle" to -3,
            "Speed with Ball" to -4,
            "Perimeter Defense" to -2,
            "Steal" to -2,
            "Speed" to -2,
            "Three-Point Shot" to -2
        ),
        "C" to mapOf(
            "Interior Defense" to 5,
            "Block" to 5,
            "Standing Dunk" to 6,
            "Offensive Rebound" to 5,
            "Defensive Rebound" to 5,
            "Strength" to 5,
            "Post Hook" to 4,
            "Post Control" to 3,
            "Close Shot" to 2,
            "Ball Handle" to -5,
            "Speed with Ball" to -6,
            "Perimeter Defense" to -4,
            "Steal" to -4,
            "Speed" to -4,
            "Agility" to -3,
            "Three-Point Shot" to -4
        )
    )

    fun generateAttributesForArchetype(
        archetype: String,
        position: String,
        startingOvr: Int
    ): Map<String, Int> {
        val archetypeDeltas = ARCHETYPE_DELTAS[archetype] ?: ARCHETYPE_DELTAS["Balanced"] ?: emptyMap()
        val positionDeltas = POSITION_ADJUSTMENTS[position] ?: emptyMap()

        return ATTRIBUTES.associate { attr ->
            val name = attr.attributeName
            val value = when (name) {
                "Stamina" -> {
                    val baseStamina = startingOvr + 14
                    val archetypeBonus = when (archetype) {
                        "Slashing Playmaker", "Two-Way Wing" -> 3
                        "Paint Beast" -> -2
                        else -> 0
                    }
                    (baseStamina + archetypeBonus).coerceIn(70, 95)
                }
                "Intangibles" -> {
                    startingOvr.coerceIn(60, 99)
                }
                "Potential" -> {
                    (startingOvr + 15).coerceIn(75, 99)
                }
                else -> {
                    val archDelta = archetypeDeltas[name] ?: 0
                    val posDelta = positionDeltas[name] ?: 0
                    (startingOvr + archDelta + posDelta).coerceIn(25, 99)
                }
            }
            name to value
        }
    }

    fun getArchetypeKeyStrengths(archetype: String): String {
        return when (archetype) {
            "Sharpshooter" -> "Key Strengths: 3PT Shot, Mid-Range, Free Throw, Shot IQ"
            "Slashing Playmaker" -> "Key Strengths: Driving Layup, Driving Dunk, Ball Handle, Speed with Ball"
            "Two-Way Wing" -> "Key Strengths: Perimeter Def, Steal, Help Def IQ, 3PT Shot"
            "Shot Creator" -> "Key Strengths: Mid-Range, Close Shot, Ball Handle, Shot IQ"
            "Paint Beast" -> "Key Strengths: Standing Dunk, Interior Def, Block, Rebounding"
            "Balanced" -> "Key Strengths: Well-rounded base with position-specific scaling"
            else -> "Key Strengths: Balanced attribute profile"
        }
    }
}
