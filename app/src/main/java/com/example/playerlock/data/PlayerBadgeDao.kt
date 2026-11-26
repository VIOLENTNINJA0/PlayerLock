package com.example.playerlock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerBadgeDao {

    @Query("SELECT * FROM player_badges WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    fun getAllBadgesFlow(profileId: Int): Flow<List<PlayerBadge>>

    @Query("SELECT * FROM player_badges WHERE profileId = :profileId ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllBadges(profileId: Int): List<PlayerBadge>

    @Query("SELECT * FROM player_badges WHERE id = :id")
    suspend fun getBadgeById(id: Int): PlayerBadge?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<PlayerBadge>)

    @Update
    suspend fun updateBadge(badge: PlayerBadge)

    @Update
    suspend fun updateBadges(badges: List<PlayerBadge>)

    @Query("SELECT COUNT(*) FROM player_badges WHERE profileId = :profileId")
    suspend fun countBadges(profileId: Int): Int

    /** Rename a badge for a specific profile (legacy migration). */
    @Query("UPDATE player_badges SET badgeName = :newName WHERE profileId = :profileId AND badgeName = :oldName")
    suspend fun renameBadge(profileId: Int, oldName: String, newName: String)

    /** Delete a badge by name that belongs to the catalog (not custom). */
    @Query("DELETE FROM player_badges WHERE profileId = :profileId AND badgeName = :badgeName")
    suspend fun deleteBadgeByName(profileId: Int, badgeName: String)

    /** Update category and sortOrder for an existing badge row. */
    @Query("UPDATE player_badges SET category = :category, sortOrder = :sortOrder WHERE profileId = :profileId AND badgeName = :badgeName")
    suspend fun updateCategoryAndSortOrder(profileId: Int, badgeName: String, category: String, sortOrder: Int)
}
