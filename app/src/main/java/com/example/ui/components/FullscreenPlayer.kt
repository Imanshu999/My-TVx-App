package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Channel
import com.example.player.TvPlayerManager
import com.example.player.TvPlayerState

@Composable
fun FullscreenPlayerView(
    playerManager: TvPlayerManager,
    playerState: TvPlayerState,
    currentChannel: Channel?,
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VideoPlayerView(
            playerManager = playerManager,
            playerState = playerState,
            currentChannel = currentChannel,
            isFullscreen = true,
            onToggleFullscreen = onExitFullscreen,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun FullscreenPlayerDialog(
    playerManager: TvPlayerManager,
    playerState: TvPlayerState,
    currentChannel: Channel?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoPlayerView(
                playerManager = playerManager,
                playerState = playerState,
                currentChannel = currentChannel,
                isFullscreen = true,
                onToggleFullscreen = onDismiss,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
