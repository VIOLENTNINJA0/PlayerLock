package com.example.playerlock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonSummaryDao {
    @Query("SELECT * FROM season_summaries WHERE profileId = :profileId ORDER BY seasonNumber DESC")
    fun getSeasonSummariesFlow(profileId: Int): Flow<List<SeasonSummary>>

    @Query("SELECT * FROM season_summaries WHERE profileId = :profileId ORDER BY seasonNumber DESC")
    suspend fun getSeasonSummaries(profileId: Int): List<SeasonSummary>

    @Query("SELECT * FROM season_summaries WHERE profileId = :profileId AND seasonNumber = :seasonNumber LIMIT 1")
    suspend fun getSeasonSummaryForSeason(profileId: Int, seasonNumber: Int): SeasonSummary?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeasonSummary(summary: SeasonSummary): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeasonSummaries(summaries: List<SeasonSummary>)

    @Query("DELETE FROM season_summaries WHERE profileId = :profileId")
    suspend fun deleteSeasonSummariesForProfile(profileId: Int)
}
