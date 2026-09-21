package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class TvPlayerManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val prefs = appContext.getSharedPreferences("tv_player_preferences", Context.MODE_PRIVATE)

    private val savedResizeMode = prefs.getInt("pref_resize_mode", AspectRatioFrameLayout.RESIZE_MODE_FIT)
    private val savedVolume = prefs.getFloat("pref_volume", 1.0f)
    private val savedMuted = prefs.getBoolean("pref_is_muted", false)

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 MyTVx/1.0")
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(20000)
        .setAllowCrossProtocolRedirects(true)
        .setKeepPostFor302Redirects(true)

    private val mediaSourceFactory = DefaultMediaSourceFactory(appContext)
        .setDataSourceFactory(httpDataSourceFactory)

    private val isEmulator: Boolean = android.os.Build.FINGERPRINT.startsWith("generic") ||
        android.os.Build.FINGERPRINT.startsWith("unknown") ||
        android.os.Build.MODEL.contains("google_sdk") ||
        android.os.Build.MODEL.contains("Emulator") ||
        android.os.Build.MODEL.contains("Android SDK built for") ||
        android.os.Build.HARDWARE.contains("goldfish") ||
        android.os.Build.HARDWARE.contains("ranchu") ||
        android.os.Build.PRODUCT.contains("sdk")

    // Configure decoder selector: prioritize OpenMAX (OMX.google.*) decoders when available
    // to bypass Codec2 (CCodec.cpp) system resource interface queries, followed by standard
    // software decoders (c2.android.*)
    private val customMediaCodecSelector = MediaCodecSelector { mimeType, requiresSecure, requiresTunneling ->
        val decoders = MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecure, requiresTunneling)
        decoders.sortedWith(compareBy(
            { decoder ->
                when {
                    decoder.name.startsWith("OMX.google.") -> 0
                    decoder.name.startsWith("OMX.") -> 1
                    decoder.name.startsWith("c2.android.") -> 2
                    decoder.softwareOnly -> 3
                    else -> 4
                }
            },
            { it.name }
        ))
    }

    // Enable decoder fallback to gracefully handle missing/exhausted hardware decoders on emulators and various devices
    private val renderersFactory = DefaultRenderersFactory(appContext).apply {
        setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
        setEnableDecoderFallback(true)
        setMediaCodecSelector(customMediaCodecSelector)
    }

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    val player: ExoPlayer = ExoPlayer.Builder(appContext, renderersFactory)
        .setMediaSourceFactory(mediaSourceFactory)
        .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
        .setWakeMode(C.WAKE_MODE_NONE)
        .build()

    private val _playerState = MutableStateFlow(
        TvPlayerState(
            resizeMode = savedResizeMode,
            volume = if (savedMuted) 0f else savedVolume,
            isMuted = savedMuted
        )
    )
    val playerState: StateFlow<TvPlayerState> = _playerState.asStateFlow()

    private var previousVolume: Float = if (savedVolume > 0f) savedVolume else 1.0f
    private var autoRetryAttemptedForUrl: String? = null

    init {
        // Enforce scaling to fit naturally without cropping (object-fit: contain equivalent)
        player.videoScalingMode = if (savedResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        } else {
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }

        player.volume = if (savedMuted) 0f else savedVolume

        player.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                _playerState.update {
                    it.copy(
                        videoWidth = videoSize.width,
                        videoHeight = videoSize.height,
                        pixelWidthHeightRatio = videoSize.pixelWidthHeightRatio
                    )
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _playerState.update { it.copy(isBuffering = true, errorMessage = null) }
                    }
                    Player.STATE_READY -> {
                        _playerState.update {
                            it.copy(
                                isBuffering = false,
                                isPlaying = player.isPlaying,
                                errorMessage = null,
                                isAutoRetrying = false
                            )
                        }
                    }
                    Player.STATE_ENDED -> {
                        _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
                    }
                    Player.STATE_IDLE -> {
                        // Keep current error or idle state
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update {
                    it.copy(isPlaying = isPlaying, isBuffering = if (isPlaying) false else it.isBuffering)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val currentUrl = _playerState.value.currentStreamUrl

                // Automatic single retry fallback for transient network dropouts
                if (currentUrl != null && autoRetryAttemptedForUrl != currentUrl) {
                    autoRetryAttemptedForUrl = currentUrl
                    _playerState.update { it.copy(isBuffering = true, isAutoRetrying = true, errorMessage = null) }
                    scope.launch {
                        delay(1200)
                        if (_playerState.value.currentStreamUrl == currentUrl) {
                            retryInternal()
                        }
                    }
                    return
                }

                val message = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Stream connection timed out"
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                        "Stream server returned an error"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                        "Stream format is unsupported or expired"
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                    PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ->
                        "Codec resource temporarily unavailable"
                    else -> "Live stream currently unavailable"
                }

                _playerState.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        isAutoRetrying = false,
                        errorMessage = message
                    )
                }
            }
        })
    }

    fun playStream(url: String) {
        if (url.isBlank()) {
            _playerState.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    isAutoRetrying = false,
                    errorMessage = "Stream URL is unavailable"
                )
            }
            return
        }

        if (url == _playerState.value.currentStreamUrl && _playerState.value.errorMessage == null) {
            if (!player.isPlaying) {
                player.play()
            }
            return
        }

        autoRetryAttemptedForUrl = null
        _playerState.update {
            it.copy(
                currentStreamUrl = url,
                errorMessage = null,
                isBuffering = true,
                isAutoRetrying = false,
                isPlaying = false
            )
        }

        try {
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .build()
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            _playerState.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    isAutoRetrying = false,
                    errorMessage = "Unable to start stream: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    private fun retryInternal() {
        val currentUrl = _playerState.value.currentStreamUrl ?: return
        try {
            val mediaItem = MediaItem.Builder()
                .setUri(currentUrl)
                .build()
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            _playerState.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    isAutoRetrying = false,
                    errorMessage = "Live stream currently unavailable"
                )
            }
        }
    }

    fun retry() {
        autoRetryAttemptedForUrl = null
        val currentUrl = _playerState.value.currentStreamUrl ?: return
        playStream(currentUrl)
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE && _playerState.value.currentStreamUrl != null) {
                retry()
            } else {
                player.play()
            }
        }
    }

    private var wasPlayingBeforeBackground = false

    fun onAppBackgrounded() {
        wasPlayingBeforeBackground = player.isPlaying
        player.pause()
    }

    fun onAppForegrounded() {
        if (wasPlayingBeforeBackground && _playerState.value.errorMessage == null) {
            player.play()
        }
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        player.volume = clamped
        if (clamped > 0f) {
            previousVolume = clamped
        }
        val isMuted = clamped == 0f
        prefs.edit()
            .putFloat("pref_volume", if (clamped > 0f) clamped else previousVolume)
            .putBoolean("pref_is_muted", isMuted)
            .apply()

        _playerState.update {
            it.copy(
                volume = clamped,
                isMuted = isMuted
            )
        }
    }

    fun toggleMute() {
        val isCurrentlyMuted = _playerState.value.isMuted || player.volume == 0f
        if (isCurrentlyMuted) {
            val restored = if (previousVolume > 0f) previousVolume else 1.0f
            player.volume = restored
            prefs.edit().putBoolean("pref_is_muted", false).apply()
            _playerState.update { it.copy(isMuted = false, volume = restored) }
        } else {
            previousVolume = if (player.volume > 0f) player.volume else 1.0f
            player.volume = 0f
            prefs.edit().putBoolean("pref_is_muted", true).apply()
            _playerState.update { it.copy(isMuted = true, volume = 0f) }
        }
    }

    fun cycleResizeMode() {
        val nextMode = when (_playerState.value.resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        setResizeMode(nextMode)
    }

    fun setResizeMode(mode: Int) {
        player.videoScalingMode = if (mode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        } else {
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
        prefs.edit().putInt("pref_resize_mode", mode).apply()
        _playerState.update { it.copy(resizeMode = mode) }
    }

    fun release() {
        scope.cancel()
        player.stop()
        player.release()
    }
}
