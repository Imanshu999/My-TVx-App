package com.example.player

import androidx.media3.ui.AspectRatioFrameLayout

data class TvPlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isMuted: Boolean = false,
    val volume: Float = 1.0f,
    val errorMessage: String? = null,
    val currentStreamUrl: String? = null,
    val resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val pixelWidthHeightRatio: Float = 1.0f,
    val isAutoRetrying: Boolean = false
)
