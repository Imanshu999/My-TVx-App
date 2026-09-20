package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_channels")
data class FavoriteChannelEntity(
    @PrimaryKey val channelId: String,
    val name: String,
    val logoUrl: String?,
    val category: String,
    val streamUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_channels")
data class RecentChannelEntity(
    @PrimaryKey val channelId: String,
    val name: String,
    val logoUrl: String?,
    val category: String,
    val streamUrl: String,
    val watchedAt: Long = System.currentTimeMillis()
)
