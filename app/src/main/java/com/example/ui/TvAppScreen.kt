package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Channel
import com.example.ui.components.CategoryChipRow
import com.example.ui.components.ChannelGridItem
import com.example.ui.components.ChannelListItem
import com.example.ui.components.CountryChipRow
import com.example.ui.components.TopNavBar
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TvNeonCyan
import com.example.ui.theme.TvRed

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun TvAppScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val displayedChannels by viewModel.displayedChannels.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recents by viewModel.recentChannels.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Natural aspect ratio calculation (e.g. 4:3, 16:9, 21:9) matching object-fit: contain
    val naturalAspectRatio = remember(playerState.videoWidth, playerState.videoHeight, playerState.pixelWidthHeightRatio) {
        if (playerState.videoWidth > 0 && playerState.videoHeight > 0) {
            val ratio = (playerState.videoWidth.toFloat() * playerState.pixelWidthHeightRatio) / playerState.videoHeight.toFloat()
            ratio.coerceIn(1.2f, 2.4f)
        } else {
            16f / 9f
        }
    }

    // Safely pause player when app goes to background
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.playerManager.pause()
    }

    // Auto-Rotate and Fullscreen System Insets Management
    DisposableEffect(uiState.isFullscreen, activity) {
        val window = activity?.window
        val insetsController = if (window != null) {
            WindowCompat.getInsetsController(window, window.decorView)
        } else null

        if (uiState.isFullscreen && activity != null && window != null) {
            // 1. Fullscreen & Auto-Rotate: Lock screen orientation to landscape
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

            // 2. Hide system status bar & navigation bar for true edge-to-edge fullscreen
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            // 3. Exit Fullscreen: Smoothly restore portrait/normal orientation
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            // Restore system bars
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Intercept Android back press to smoothly exit fullscreen
    if (uiState.isFullscreen) {
        BackHandler {
            viewModel.setFullscreen(false)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(if (uiState.isFullscreen) Color.Black else DarkBackground)
            .then(
                if (uiState.isFullscreen) Modifier else Modifier.statusBarsPadding().navigationBarsPadding()
            ),
        containerColor = if (uiState.isFullscreen) Color.Black else DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (uiState.isFullscreen) Modifier else Modifier.padding(innerPadding)
                )
        ) {
            // 1. Top Bar (Hidden in Fullscreen)
            if (!uiState.isFullscreen) {
                TopNavBar(
                    isSearching = uiState.isSearching,
                    searchQuery = uiState.searchQuery,
                    isGridView = isGridView,
                    isLoading = uiState.isLoadingChannels,
                    totalChannels = displayedChannels.size,
                    onSearchToggle = { viewModel.toggleSearching() },
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    onViewModeToggle = { viewModel.toggleViewMode() },
                    onRefresh = { viewModel.loadChannels(forceRefresh = true) }
                )
            }

            // 2. Video Player View (Maintained continuously across orientation & fullscreen changes)
            Box(
                modifier = if (uiState.isFullscreen) {
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .testTag("fullscreen_video_container")
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(naturalAspectRatio)
                        .background(Color.Black)
                        .animateContentSize()
                        .testTag("video_player_container_box")
                },
                contentAlignment = Alignment.Center
            ) {
                VideoPlayerView(
                    playerManager = viewModel.playerManager,
                    playerState = playerState,
                    currentChannel = uiState.currentChannel,
                    isFullscreen = uiState.isFullscreen,
                    onToggleFullscreen = { viewModel.setFullscreen(!uiState.isFullscreen) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Content below player (Hidden in Fullscreen mode)
            if (!uiState.isFullscreen) {
                // 3. Category Filter Chips (e.g., News, Sports, Movies, Music, Kids, Favorites, Recent)
                CategoryChipRow(
                    categories = uiState.categories.map { item ->
                        when (item.name) {
                            "Favorites" -> item.copy(count = favorites.size)
                            "Recent" -> item.copy(count = recents.size)
                            else -> item
                        }
                    },
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { viewModel.selectCategory(it) }
                )

            // 3b. Country / Region Filter Chips (e.g. All, US, GB, CA, etc.)
            CountryChipRow(
                countries = uiState.countries,
                selectedCountry = uiState.selectedCountry,
                onCountrySelected = { viewModel.selectCountry(it) }
            )

            // 4. Section Subheader: Category & Region Title & Results Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val activeTitle = when {
                        uiState.searchQuery.isNotEmpty() -> "Search Results"
                        uiState.selectedCategory == "Favorites" -> "★ Favorite Channels"
                        uiState.selectedCategory == "Recent" -> "⏱ Recently Watched"
                        else -> uiState.selectedCategory
                    }

                    Text(
                        text = activeTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    if (uiState.selectedCountry != "ALL") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${uiState.selectedCountry})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TvNeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${displayedChannels.size} channels)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                if (uiState.searchQuery.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.onSearchQueryChange("") },
                        modifier = Modifier.testTag("clear_search_query_button")
                    ) {
                        Text("Clear Search", color = TvNeonCyan, fontSize = 12.sp)
                    }
                } else if (uiState.selectedCountry != "ALL") {
                    TextButton(
                        onClick = { viewModel.selectCountry("ALL") },
                        modifier = Modifier.testTag("reset_country_filter_button")
                    ) {
                        Text("All Regions", color = TvNeonCyan, fontSize = 12.sp)
                    }
                }
            }

            // 5. Channel List or Grid / Empty / Loading States
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoadingChannels && displayedChannels.isEmpty() -> {
                        // Loading State
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = TvRed,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading channels from iptv-org...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Extracting names, categories, logos, and streams",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    uiState.playlistError != null && displayedChannels.isEmpty() -> {
                        // Error Loading Playlist
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = TvRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Failed to load channels",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.playlistError ?: "Network error",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadChannels(forceRefresh = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = TvRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Connection")
                            }
                        }
                    }

                    displayedChannels.isEmpty() -> {
                        // Empty Filter / Search State
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.selectedCategory == "Favorites") Icons.Default.Favorite else Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = when {
                                    uiState.selectedCategory == "Favorites" -> "No favorites saved yet"
                                    uiState.selectedCategory == "Recent" -> "No watched history yet"
                                    uiState.searchQuery.isNotEmpty() -> "No channels matching '${uiState.searchQuery}'"
                                    else -> "No channels in this category"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when {
                                    uiState.selectedCategory == "Favorites" -> "Tap the heart icon on any channel card to save it for quick access."
                                    uiState.selectedCategory == "Recent" -> "Channels you watch will show up here automatically."
                                    else -> "Try adjusting your search query or selecting another category."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    isGridView -> {
                        // Grid View
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("channel_grid")
                        ) {
                            items(
                                items = displayedChannels,
                                key = { it.id }
                            ) { channel ->
                                ChannelGridItem(
                                    channel = channel,
                                    isPlaying = uiState.currentChannel?.id == channel.id,
                                    onChannelClick = { viewModel.selectChannel(it) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) }
                                )
                            }
                        }
                    }

                    else -> {
                        // List View
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("channel_list")
                        ) {
                            items(
                                items = displayedChannels,
                                key = { it.id }
                            ) { channel ->
                                ChannelListItem(
                                    channel = channel,
                                    isPlaying = uiState.currentChannel?.id == channel.id,
                                    onChannelClick = { viewModel.selectChannel(it) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

