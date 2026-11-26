package com.example.playerlock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerAttributeDao {

    @Query("SELECT * FROM player_attributes WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    fun getAllAttributes(profileId: Int): Flow<List<PlayerAttribute>>

    @Query("SELECT * FROM player_attributes WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllAttributesList(profileId: Int): List<PlayerAttribute>

    @Query("SELECT * FROM player_attributes WHERE id = :id")
    suspend fun getAttributeById(id: Int): PlayerAttribute?

    @Query("SELECT COUNT(*) FROM player_attributes WHERE profileId = :profileId")
    suspend fun countAttributes(profileId: Int): Int

    @Query("SELECT * FROM player_attributes WHERE profileId = :profileId AND category = :category ORDER BY sortOrder ASC, id ASC")
    suspend fun getAttributesByCategory(profileId: Int, category: String): List<PlayerAttribute>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttributes(attributes: List<PlayerAttribute>)

    @Update
    suspend fun updateAttribute(attribute: PlayerAttribute)

    @Update
    suspend fun updateAttributes(attributes: List<PlayerAttribute>)

    @Query("DELETE FROM player_attributes WHERE attributeName = 'Overall Durability' AND profileId = :profileId")
    suspend fun deleteOverallDurability(profileId: Int)
}
