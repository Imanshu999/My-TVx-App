package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CountryHelper
import com.example.data.PlaylistRepository
import com.example.data.local.FavoriteChannelEntity
import com.example.data.local.RecentChannelEntity
import com.example.data.local.TvDatabase
import com.example.model.Channel
import com.example.model.CountryItem
import com.example.player.TvPlayerManager
import com.example.player.TvPlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val selectedCategory: String = "All",
    val selectedCountry: String = "ALL", // "ALL" or 2-letter ISO code e.g. "US"
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val isGridView: Boolean = true,
    val currentChannel: Channel? = null,
    val isFullscreen: Boolean = false,
    val isLoadingChannels: Boolean = true,
    val playlistError: String? = null,
    val categories: List<CategoryItem> = listOf(CategoryItem("All", 0)),
    val countries: List<CountryItem> = listOf(CountryItem("ALL", "All Countries", "🌐", 0))
)

data class CategoryItem(
    val name: String,
    val count: Int
)

data class ChannelFilterCriteria(
    val category: String = "All",
    val country: String = "ALL",
    val searchQuery: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("tv_user_preferences", Context.MODE_PRIVATE)

    // Persisted View Mode (Grid vs List Layout)
    private val _isGridView = MutableStateFlow(prefs.getBoolean("pref_is_grid_view", true))
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val savedCategory = prefs.getString("pref_selected_category", "All") ?: "All"
    private val savedCountry = prefs.getString("pref_selected_country", "ALL") ?: "ALL"
    private val savedLastChannelId = prefs.getString("pref_last_channel_id", null)

    private val db = TvDatabase.getInstance(application)
    private val repository = PlaylistRepository(application, db.tvDao())
    val playerManager = TvPlayerManager(application)

    val playerState: StateFlow<TvPlayerState> = playerManager.playerState

    private val _uiState = MutableStateFlow(
        MainUiState(
            isGridView = _isGridView.value,
            selectedCategory = savedCategory,
            selectedCountry = savedCountry
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _rawChannels = MutableStateFlow<List<Channel>>(emptyList())

    // Dedicated filter criteria flow so visual state toggles don't re-filter channels
    private val _filterCriteria = MutableStateFlow(
        ChannelFilterCriteria(
            category = savedCategory,
            country = savedCountry
        )
    )

    val favorites: StateFlow<List<FavoriteChannelEntity>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentChannels: StateFlow<List<RecentChannelEntity>> = repository.recentFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined filtered channel list - only recomputes when channels or filters change
    val displayedChannels: StateFlow<List<Channel>> = combine(
        _rawChannels,
        _filterCriteria,
        favorites,
        recentChannels
    ) { channels, criteria, favs, recents ->
        val favoriteIds = favs.map { it.channelId }.toSet()
        val recentIdsMap = recents.associate { it.channelId to it.watchedAt }

        // Map channels with updated favorite and recent flags
        val enriched = channels.map { channel ->
            channel.copy(
                isFavorite = favoriteIds.contains(channel.id),
                lastWatchedTimestamp = recentIdsMap[channel.id]
            )
        }

        // Apply Country Filter first (if not ALL)
        val countryFiltered = if (criteria.country == "ALL") {
            enriched
        } else if (criteria.country == "INT") {
            enriched.filter { it.countryCode == null }
        } else {
            enriched.filter { it.countryCode.equals(criteria.country, ignoreCase = true) }
        }

        // Apply Category Filter
        val categoryFiltered = when (criteria.category) {
            "All" -> countryFiltered
            "Favorites" -> countryFiltered.filter { it.isFavorite }
            "Recent" -> {
                val recentsOrdered = recents.mapNotNull { r ->
                    countryFiltered.find { it.id == r.channelId }
                }
                recentsOrdered
            }
            else -> countryFiltered.filter { it.category.equals(criteria.category, ignoreCase = true) }
        }

        // Apply Search Filter
        val trimmedQuery = criteria.searchQuery.trim().lowercase()
        if (trimmedQuery.isEmpty()) {
            categoryFiltered
        } else {
            categoryFiltered.filter {
                it.name.lowercase().contains(trimmedQuery) ||
                it.category.lowercase().contains(trimmedQuery) ||
                it.rawCategory.lowercase().contains(trimmedQuery) ||
                it.countryName.lowercase().contains(trimmedQuery) ||
                (it.countryCode?.lowercase()?.contains(trimmedQuery) == true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadChannels()
    }

    fun loadChannels(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChannels = true, playlistError = null) }
            val result = repository.fetchChannels(forceRefresh)
            result.onSuccess { channels ->
                _rawChannels.value = channels

                // Generate categories with channel counts
                val categoryMap = channels.groupingBy { it.category }.eachCount()
                val sortedCategories = categoryMap.entries
                    .sortedByDescending { it.value }
                    .map { CategoryItem(it.key, it.value) }

                val fullCategories = mutableListOf<CategoryItem>()
                fullCategories.add(CategoryItem("All", channels.size))
                fullCategories.add(CategoryItem("Favorites", 0))
                fullCategories.add(CategoryItem("Recent", 0))
                fullCategories.addAll(sortedCategories)

                // Generate countries with channel counts
                val countryMap = mutableMapOf<String, Int>()
                var internationalCount = 0
                for (ch in channels) {
                    val code = ch.countryCode
                    if (code != null) {
                        countryMap[code] = (countryMap[code] ?: 0) + 1
                    } else {
                        internationalCount++
                    }
                }

                val sortedCountries = countryMap.entries
                    .sortedByDescending { it.value }
                    .map {
                        CountryItem(
                            code = it.key,
                            name = CountryHelper.getCountryName(it.key),
                            flagEmoji = CountryHelper.getFlagEmoji(it.key),
                            count = it.value
                        )
                    }

                val fullCountries = mutableListOf<CountryItem>()
                fullCountries.add(CountryItem("ALL", "All Countries", "🌐", channels.size))
                fullCountries.addAll(sortedCountries)
                if (internationalCount > 0) {
                    fullCountries.add(CountryItem("INT", "International", "🌍", internationalCount))
                }

                val initialChannel = if (savedLastChannelId != null) {
                    channels.find { it.id == savedLastChannelId } ?: channels.firstOrNull()
                } else {
                    channels.firstOrNull()
                }

                _uiState.update {
                    it.copy(
                        isLoadingChannels = false,
                        categories = fullCategories,
                        countries = fullCountries,
                        currentChannel = it.currentChannel ?: initialChannel
                    )
                }

                // Start playback if not already active
                if (playerManager.playerState.value.currentStreamUrl == null && initialChannel != null) {
                    playerManager.playStream(initialChannel.streamUrl)
                    repository.recordRecent(initialChannel)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingChannels = false,
                        playlistError = error.message ?: "Failed to load playlist"
                    )
                }
            }
        }
    }

    fun selectChannel(channel: Channel) {
        prefs.edit().putString("pref_last_channel_id", channel.id).apply()
        _uiState.update { it.copy(currentChannel = channel) }
        playerManager.playStream(channel.streamUrl)
        viewModelScope.launch {
            repository.recordRecent(channel)
        }
    }

    fun selectNextChannel() {
        val list = displayedChannels.value
        if (list.isEmpty()) return
        val current = _uiState.value.currentChannel
        val currentIndex = if (current != null) list.indexOfFirst { it.id == current.id } else -1
        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % list.size else 0
        selectChannel(list[nextIndex])
    }

    fun selectPreviousChannel() {
        val list = displayedChannels.value
        if (list.isEmpty()) return
        val current = _uiState.value.currentChannel
        val currentIndex = if (current != null) list.indexOfFirst { it.id == current.id } else -1
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        selectChannel(list[prevIndex])
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            val isFav = favorites.value.any { it.channelId == channel.id }
            repository.toggleFavorite(channel, isFav)
        }
    }

    fun selectCategory(category: String) {
        prefs.edit().putString("pref_selected_category", category).apply()
        _filterCriteria.update { it.copy(category = category) }
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun selectCountry(countryCode: String) {
        prefs.edit().putString("pref_selected_country", countryCode).apply()
        _filterCriteria.update { it.copy(country = countryCode) }
        _uiState.update { it.copy(selectedCountry = countryCode) }
    }

    fun onSearchQueryChange(query: String) {
        _filterCriteria.update { it.copy(searchQuery = query) }
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearching() {
        _uiState.update {
            val next = !it.isSearching
            if (!next) {
                _filterCriteria.update { c -> c.copy(searchQuery = "") }
            }
            it.copy(
                isSearching = next,
                searchQuery = if (!next) "" else it.searchQuery
            )
        }
    }

    fun toggleViewMode() {
        val next = !_isGridView.value
        _isGridView.value = next
        prefs.edit().putBoolean("pref_is_grid_view", next).apply()
        _uiState.update { it.copy(isGridView = next) }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(isFullscreen = fullscreen) }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
