package com.example.playerlock.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UpgradeHistoryDao {

    @Query("SELECT * FROM upgrade_history WHERE profileId = :profileId AND entityType = :entityType AND entityId = :entityId ORDER BY timestamp DESC")
    suspend fun getHistoryForEntity(profileId: Int, entityType: String, entityId: Int): List<UpgradeHistory>

    @Query("SELECT * FROM upgrade_history WHERE profileId = :profileId AND isAppliedIn2k = 0 ORDER BY timestamp DESC")
    fun getUnappliedHistoryFlow(profileId: Int): Flow<List<UpgradeHistory>>

    @Query("SELECT * FROM upgrade_history WHERE profileId = :profileId AND isAppliedIn2k = 0 ORDER BY timestamp DESC")
    suspend fun getUnappliedHistory(profileId: Int): List<UpgradeHistory>

    @Query("SELECT * FROM upgrade_history WHERE profileId = :profileId ORDER BY timestamp ASC")
    suspend fun getAllUpgradeHistory(profileId: Int): List<UpgradeHistory>

    @Query("SELECT * FROM upgrade_history WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getAllUpgradeHistoryFlow(profileId: Int): Flow<List<UpgradeHistory>>

    @Query("SELECT * FROM upgrade_history WHERE id = :historyId")
    suspend fun getHistoryById(historyId: Int): UpgradeHistory?

    @Query("UPDATE upgrade_history SET isAppliedIn2k = 1 WHERE id = :historyId")
    suspend fun markUpgradeAppliedIn2k(historyId: Int): Int

    @Query("UPDATE upgrade_history SET isAppliedIn2k = 1 WHERE profileId = :profileId AND isAppliedIn2k = 0")
    suspend fun markAllAppliedIn2k(profileId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpgradeHistory(history: UpgradeHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpgradeHistory(histories: List<UpgradeHistory>)

    @Delete
    suspend fun deleteUpgradeHistory(history: UpgradeHistory)
}
