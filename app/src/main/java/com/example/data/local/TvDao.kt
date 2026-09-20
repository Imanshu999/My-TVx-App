package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TvDao {
    @Query("SELECT * FROM favorite_channels ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteChannelEntity)

    @Query("DELETE FROM favorite_channels WHERE channelId = :channelId")
    suspend fun deleteFavoriteById(channelId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_channels WHERE channelId = :channelId)")
    suspend fun isFavorite(channelId: String): Boolean

    @Query("SELECT * FROM recent_channels ORDER BY watchedAt DESC LIMIT 20")
    fun getRecentChannels(): Flow<List<RecentChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentChannelEntity)

    @Query("DELETE FROM recent_channels WHERE channelId = :channelId")
    suspend fun deleteRecentById(channelId: String)

    @Query("DELETE FROM recent_channels")
    suspend fun clearRecent()
}
