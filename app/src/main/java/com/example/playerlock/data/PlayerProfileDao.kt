package com.example.playerlock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerProfileDao {

    @Query("SELECT * FROM player_profiles ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<PlayerProfile>>

    @Query("SELECT * FROM player_profiles WHERE id = :id")
    suspend fun getProfileById(id: Int): PlayerProfile?

    @Query("SELECT * FROM player_profiles WHERE id = :id")
    fun getProfileFlowById(id: Int): Flow<PlayerProfile?>

    @Query("SELECT * FROM player_profiles ORDER BY id ASC LIMIT 1")
    suspend fun getDefaultProfile(): PlayerProfile?

    @Query("SELECT * FROM player_profiles ORDER BY id ASC LIMIT 1")
    fun getDefaultProfileFlow(): Flow<PlayerProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: PlayerProfile): Long

    @Update
    suspend fun updateProfile(profile: PlayerProfile)

    @Query("DELETE FROM player_profiles WHERE id = :id")
    suspend fun deleteProfileOnly(id: Int)

    @Query("DELETE FROM player_attributes WHERE profileId = :id")
    suspend fun deleteAttributesForProfile(id: Int)

    @Query("DELETE FROM player_badges WHERE profileId = :id")
    suspend fun deleteBadgesForProfile(id: Int)

    @Query("DELETE FROM game_logs WHERE profileId = :id")
    suspend fun deleteGameLogsForProfile(id: Int)

    @Query("DELETE FROM upgrade_history WHERE profileId = :id")
    suspend fun deleteUpgradeHistoryForProfile(id: Int)

    @Query("DELETE FROM season_summaries WHERE profileId = :id")
    suspend fun deleteSeasonSummariesForProfile(id: Int)

    @Transaction
    suspend fun deleteProfileById(id: Int) {
        deleteAttributesForProfile(id)
        deleteBadgesForProfile(id)
        deleteGameLogsForProfile(id)
        deleteUpgradeHistoryForProfile(id)
        deleteSeasonSummariesForProfile(id)
        deleteProfileOnly(id)
    }
}
