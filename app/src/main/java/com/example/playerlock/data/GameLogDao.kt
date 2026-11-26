package com.example.playerlock.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameLogDao {

    @Query("SELECT * FROM game_logs WHERE profileId = :profileId ORDER BY datePlayed DESC, id DESC")
    fun getAllGameLogsFlow(profileId: Int): Flow<List<GameLog>>

    @Query("SELECT * FROM game_logs WHERE profileId = :profileId ORDER BY datePlayed DESC, id DESC")
    suspend fun getAllGameLogs(profileId: Int): List<GameLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameLog(log: GameLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameLogs(logs: List<GameLog>)

    @Delete
    suspend fun deleteGameLog(log: GameLog)
}
