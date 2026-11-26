package com.example.playerlock.data

object BadgeGlossaryData {

    val DESCRIPTIONS: Map<String, String> = mapOf(
        // ── Finishing ──────────────────────────────────────────────────────
        "Aerial Wizard" to "Increases ability to finish an alley-oop dunk from a teammate, or an in-air putback dunk off an offensive rebound.",
        "Float Game" to "Increases a player's ability to finish floaters.",
        "Ghost Stepper" to "Increases the effectiveness of step-through layups or other close-range shots that come after a shot fake.",
        "Hook Specialist" to "Improves ability to make post hooks.",
        "Layup Mixmaster" to "Improves a player's ability to finish fancy or acrobatic layups.",
        "Paint Prodigy" to "Improves a player's ability to quickly and effectively score while going to work in the paint.",
        "Physical Finisher" to "Improves a player's ability to battle through contact and convert contact layups.",
        "Post Powerhouse" to "Strengthens a player's ability at backing down defenders and moving them with drop steps.",
        "Post Spin Catalyst" to "Attempting post spins or drives results in more effective moves and less of a chance to be stripped.",
        "Posterizer" to "Increases the chances of throwing down a dunk on your defender while driving to the hoop.",
        "Rise Up" to "Increases the likelihood of dunking on your opponent when standing in the painted area.",

        // ── Shooting ───────────────────────────────────────────────────────
        "Arc Cadence" to "Improves a player's ability to knock down 3-point shots on the move, including pull-ups, hop-steps, or spin shots.",
        "Deadeye" to "Jump shots taken with a defender closing out receive less of a penalty from a shot contest.",
        "Limitless Range" to "Extends the range from which a player can shoot 3PT shots effectively from deep.",
        "Mini Marksman" to "Shorter shooters are less bothered by lengthy defenders.",
        "Post Fade Phenom" to "Improves ability to make post fades and hop shots.",
        "Quick Trigger" to "Improves a player's ability to make no-dip jump shots.",
        "Set and Fire" to "Applies a shooting boost to 3-point shots when the player's feet are completely set.",
        "Smooth Operator" to "Improves a player's ability to knock down mid-range shots on the move, including pull-ups, hop-steps, or spin shots.",
        "Static Middy" to "Applies a shooting boost to stand-still mid-range shots.",
        "Slippery Off-Ball" to "When attempting to get open off screens, the player more effectively navigates through traffic.",

        // ── Playmaking ─────────────────────────────────────────────────────
        "Ankle Assassin" to "Increases the ability to break down the defender and cross them up.",
        "Bail Out" to "Passing out of a jump shot or layup yields fewer errant passes than normal. Additionally helps passing out of double teams.",
        "Break Starter" to "After grabbing a defensive rebound, deep outlet passes made up the court are more accurate. Passes must be made quickly following the defensive rebound.",
        "Dimer" to "Boosts the shot percentage for open teammates after catching a pass.",
        "Handles for Days" to "Allows a player to take less of an energy hit when performing consecutive dribble moves.",
        "Lightning Launch" to "Speeds up launches when attacking from the perimeter.",
        "Pace" to "Raises the speed of the ball handler when sprinting.",
        "Strong Handle" to "Reduces the likelihood of being bothered by defenders when dribbling.",
        "Unpluckable" to "Defenders have a tougher time poking the ball free with their steal attempts.",
        "Versatile Visionary" to "Improves a player's ability to thread and fire tight passes, including alley-oops, quickly and on time.",

        // ── Defense ────────────────────────────────────────────────────────
        "Ankle Braces" to "Lowers the likelihood of getting ankle-broken by opposing dribble moves.",
        "Challenger" to "Improves the effectiveness of well-timed contests against perimeter shooters.",
        "Glove" to "Increases the ability to successfully steal from ball handlers or strip layup attempts.",
        "High-Flying Denier" to "Boosts the speed and leaping ability of a defensive player in anticipation of a chase-down block attempt.",
        "Immovable Enforcer" to "Improves a defensive player's strength when defending ball handlers and finishers coming straight at them.",
        "Interceptor" to "The frequency of successfully tipped or intercepted passes greatly increases.",
        "Off-Ball Pest" to "Makes players more difficult to get past when playing off-ball, as they can grab and hold their matchup.",
        "Paint Patroller" to "Increases a player's ability to block or contest shots at the rim.",
        "Pick Dodger" to "Improves a player's ability to navigate through and around screens while on defense.",
        "Post Lockdown" to "Strengthens a player's ability to effectively defend moves in the post, with an increased chance of stripping the opponent.",
        "Seatbelt" to "Boost body-up defense effectiveness, making it easier to clamp opponents and disrupt their drive path.",
        "Wall Up" to "Improves shot contest effectiveness during hands-up defense when guarding the paint.",

        // ── Rebounding ─────────────────────────────────────────────────────
        "Boxout Boss" to "Improves the effectiveness of boxouts as the boxer.",
        "Breaker" to "Increases the success rate of breaking through opponent boxouts.",
        "Crasher" to "Boost a player's ability to track down or grab rebounds on the offensive end.",
        "Possession Closer" to "Boost a player's ability to grab rebounds on the defensive end.",
        "Sync Snatcher" to "Gives an additional Vertical and Rebounding boost when perfectly timing the jump.",

        // ── Physicals ──────────────────────────────────────────────────────
        "Brick Wall" to "Increases the effectiveness of screens.",
        "Bruiser" to "Drains energy from opponents when making physical contact.",
        "Flash" to "Speeds up movement without the ball while in transition on offense or defense.",
        "Pogo Stick" to "Allows players to quickly go back up for another jump upon landing. This applies after a rebound, block attempt, or jump shot.",
        "Work Horse" to "Increases a player's speed and ability to get loose balls over the opponent."
    )

    fun descriptionFor(badgeName: String): String =
        DESCRIPTIONS[badgeName] ?: ""
}
