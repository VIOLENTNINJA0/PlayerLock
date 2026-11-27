package com.example.playerlock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlayerProfile::class,
        PlayerAttribute::class,
        PlayerBadge::class,
        GameLog::class,
        UpgradeHistory::class,
        PlayerAward::class,
        SeasonSummary::class
    ],
    version = 12,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playerProfileDao(): PlayerProfileDao
    abstract fun playerAttributeDao(): PlayerAttributeDao
    abstract fun playerBadgeDao(): PlayerBadgeDao
    abstract fun gameLogDao(): GameLogDao
    abstract fun upgradeHistoryDao(): UpgradeHistoryDao
    abstract fun playerAwardDao(): PlayerAwardDao
    abstract fun seasonSummaryDao(): SeasonSummaryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** Migration 1→2: adds profileId column to player_attributes, player_badges, game_logs, and upgrade_history. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL(
                        "ALTER TABLE player_badges ADD COLUMN category TEXT NOT NULL DEFAULT 'Other'"
                    )
                } catch (_: Exception) {
                    // Category column may already exist
                }
                db.execSQL(
                    "ALTER TABLE player_attributes ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE player_badges ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE game_logs ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE upgrade_history ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        /** Migration 2→3: adds avatarPath column to player_profiles. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE player_profiles ADD COLUMN avatarPath TEXT"
                )
            }
        }

        /** Migration 3→4: adds heightInches, weightLbs, jerseyNumber, and teamName to player_profiles. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE player_profiles ADD COLUMN heightInches INTEGER NOT NULL DEFAULT 78"
                )
                db.execSQL(
                    "ALTER TABLE player_profiles ADD COLUMN weightLbs INTEGER NOT NULL DEFAULT 210"
                )
                db.execSQL(
                    "ALTER TABLE player_profiles ADD COLUMN jerseyNumber INTEGER NOT NULL DEFAULT 23"
                )
                db.execSQL(
                    "ALTER TABLE player_profiles ADD COLUMN teamName TEXT NOT NULL DEFAULT 'Draft Prospect'"
                )
            }
        }

        /** Migration 4→5: creates player_awards table for season & career awards. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `player_awards` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `profileId` INTEGER NOT NULL,
                        `awardName` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `seasonNumber` INTEGER NOT NULL,
                        `xpGranted` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** Migration 5→6: adds freeThrowsAttempted and freeThrowsMade to game_logs. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE game_logs ADD COLUMN freeThrowsAttempted INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE game_logs ADD COLUMN freeThrowsMade INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /** Migration 6→7: adds sortOrder column to player_attributes. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE player_attributes ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /** Migration 7→8: adds sortOrder column to player_badges. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE player_badges ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /** Migration 8→9: season tracking & season_summaries table. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE player_profiles ADD COLUMN currentSeason INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE player_profiles ADD COLUMN seasonStartOvr INTEGER NOT NULL DEFAULT 60")
                db.execSQL("ALTER TABLE player_profiles ADD COLUMN isInPlayoffs INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE game_logs ADD COLUMN seasonNumber INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE upgrade_history ADD COLUMN seasonNumber INTEGER NOT NULL DEFAULT 1")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `season_summaries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `profileId` INTEGER NOT NULL,
                        `seasonNumber` INTEGER NOT NULL,
                        `wins` INTEGER NOT NULL,
                        `losses` INTEGER NOT NULL,
                        `gamesPlayed` INTEGER NOT NULL,
                        `ppg` REAL NOT NULL,
                        `rpg` REAL NOT NULL,
                        `apg` REAL NOT NULL,
                        `spg` REAL NOT NULL,
                        `bpg` REAL NOT NULL,
                        `fgPct` REAL NOT NULL,
                        `ftPct` REAL NOT NULL,
                        `doubleDoubles` INTEGER NOT NULL,
                        `tripleDoubles` INTEGER NOT NULL,
                        `seasonHighSummary` TEXT NOT NULL,
                        `startOvr` INTEGER NOT NULL,
                        `endOvr` INTEGER NOT NULL,
                        `attributeUpgradesSummary` TEXT NOT NULL,
                        `badgeUpgradesSummary` TEXT NOT NULL,
                        `awardsSummary` TEXT NOT NULL,
                        `totalXpEarned` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration 9→10:
         * - Adds `playoffBracketJson` column to player_profiles.
         * - Adds `playoffResult` column to season_summaries.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // playoffBracketJson may already exist from a prior dev build; guard safely.
                try {
                    db.execSQL("ALTER TABLE player_profiles ADD COLUMN playoffBracketJson TEXT")
                } catch (_: Exception) {}
                // playoffResult for the SeasonSummary
                try {
                    db.execSQL("ALTER TABLE season_summaries ADD COLUMN playoffResult TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        /**
         * Migration 10→11:
         * - Adds `minutesPlayed` column to game_logs.
         * - Adds `mpg` and `totalMinutes` columns to season_summaries.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE game_logs ADD COLUMN minutesPlayed INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE season_summaries ADD COLUMN mpg REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE season_summaries ADD COLUMN totalMinutes INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        /**
         * Migration 11→12:
         * - Adds per-profile theme fields and weightAllowanceLbs to player_profiles.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE player_profiles ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'DARK'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE player_profiles ADD COLUMN selectedTeamTheme TEXT")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE player_profiles ADD COLUMN amoledBlack INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE player_profiles ADD COLUMN customPrimaryHex INTEGER NOT NULL DEFAULT -19712")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE player_profiles ADD COLUMN customSecondaryHex INTEGER NOT NULL DEFAULT -14775835")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE player_profiles ADD COLUMN customBackgroundHex INTEGER NOT NULL DEFAULT -15592942")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE player_profiles ADD COLUMN customSurfaceHex INTEGER NOT NULL DEFAULT -14803426")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE player_profiles ADD COLUMN customTextHex INTEGER NOT NULL DEFAULT -1")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE player_profiles ADD COLUMN weightAllowanceLbs INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "playerlock_database"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                        MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                        MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                        MIGRATION_10_11, MIGRATION_11_12
                    )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedDatabase() {
            val db = INSTANCE ?: error("Database not initialized. Call getDatabase first.")
            // Insert default player profile if none exists
            var defaultProfile = db.playerProfileDao().getDefaultProfile()
            if (defaultProfile == null) {
                val newId = db.playerProfileDao().insertProfile(PlayerProfile(id = 1)).toInt()
                defaultProfile = db.playerProfileDao().getProfileById(newId)
            }
            val profileId = defaultProfile?.id ?: 1
            // Pre-seed all attributes if not already seeded for default profileId
            if (db.playerAttributeDao().countAttributes(profileId) == 0) {
                db.playerAttributeDao().insertAttributes(
                    AttributeSeedData.ATTRIBUTES.map { it.copy(id = 0, profileId = profileId) }
                )
            }
            // Pre-seed the full badge catalog if not already seeded for default profileId
            if (db.playerBadgeDao().countBadges(profileId) == 0) {
                db.playerBadgeDao().insertBadges(
                    BadgeSeedData.BADGES.map { it.copy(id = 0, profileId = profileId) }
                )
            }
        }
    }
}
