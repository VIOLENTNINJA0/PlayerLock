package com.example.playerlock.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerAwardDao {

    @Query("SELECT * FROM player_awards WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getAllAwardsFlow(profileId: Int): Flow<List<PlayerAward>>

    @Query("SELECT * FROM player_awards WHERE profileId = :profileId ORDER BY timestamp DESC")
    suspend fun getAllAwards(profileId: Int): List<PlayerAward>

    @Query("SELECT * FROM player_awards WHERE id = :id")
    suspend fun getAwardById(id: Int): PlayerAward?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAward(award: PlayerAward): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAwards(awards: List<PlayerAward>)

    @Delete
    suspend fun deleteAward(award: PlayerAward)

    @Query("DELETE FROM player_awards WHERE profileId = :profileId")
    suspend fun deleteAllAwardsForProfile(profileId: Int)
}
