package com.example.model

data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String? = null,
    val category: String = "General",
    val rawCategory: String = "General",
    val streamUrl: String,
    val resolution: String? = null,
    val countryCode: String? = null,
    val countryName: String = "International",
    val isFavorite: Boolean = false,
    val lastWatchedTimestamp: Long? = null
)
