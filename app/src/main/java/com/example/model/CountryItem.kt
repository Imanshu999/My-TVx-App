package com.example.model

data class CountryItem(
    val code: String, // e.g. "ALL", "US", "GB", "INT"
    val name: String, // e.g. "All Countries", "United States", "International"
    val flagEmoji: String, // e.g. "🌐", "🇺🇸"
    val count: Int
)
