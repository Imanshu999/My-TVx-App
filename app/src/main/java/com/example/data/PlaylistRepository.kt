package com.example.data

import android.content.Context
import com.example.data.local.FavoriteChannelEntity
import com.example.data.local.RecentChannelEntity
import com.example.data.local.TvDao
import com.example.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class PlaylistRepository(
    private val context: Context,
    private val tvDao: TvDao
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val cacheFile = File(context.cacheDir, "iptv_index_cache.m3u")

    val favoritesFlow: Flow<List<FavoriteChannelEntity>> = tvDao.getAllFavorites()
    val recentFlow: Flow<List<RecentChannelEntity>> = tvDao.getRecentChannels()

    suspend fun fetchChannels(forceRefresh: Boolean = false): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            // If we have cache and not forcing refresh, load cache first for instant UX
            if (!forceRefresh && cacheFile.exists() && cacheFile.length() > 1024) {
                try {
                    val channels = FileInputStream(cacheFile).use { M3uParser.parse(it) }
                    if (channels.isNotEmpty()) {
                        return@withContext Result.success(channels)
                    }
                } catch (_: Exception) {
                    // fall back to network fetch
                }
            }

            // Fetch from exact URL
            val request = Request.Builder()
                .url("https://iptv-org.github.io/iptv/index.m3u")
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile) MyTVx/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                // If network fails but cache exists, return cache
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    val cached = FileInputStream(cacheFile).use { M3uParser.parse(it) }
                    return@withContext Result.success(cached)
                }
                return@withContext Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
            }

            val body = response.body ?: throw Exception("Empty response body from playlist URL")

            // Write to cache file atomically
            val tempFile = File(context.cacheDir, "iptv_index_temp.m3u")
            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                if (cacheFile.exists()) {
                    cacheFile.delete()
                }
                if (!tempFile.renameTo(cacheFile)) {
                    tempFile.copyTo(cacheFile, overwrite = true)
                    tempFile.delete()
                }
            }

            val channels = FileInputStream(cacheFile).use { M3uParser.parse(it) }
            if (channels.isEmpty()) {
                Result.success(getFallbackChannels())
            } else {
                Result.success(channels)
            }
        } catch (e: Exception) {
            // Check if we can fallback to cache
            if (cacheFile.exists() && cacheFile.length() > 0) {
                try {
                    val cached = FileInputStream(cacheFile).use { M3uParser.parse(it) }
                    if (cached.isNotEmpty()) {
                        return@withContext Result.success(cached)
                    }
                } catch (_: Exception) { }
            }
            // Return fallback default channels so app is never dead
            Result.success(getFallbackChannels())
        }
    }

    suspend fun toggleFavorite(channel: Channel, isCurrentlyFavorite: Boolean) = withContext(Dispatchers.IO) {
        if (isCurrentlyFavorite) {
            tvDao.deleteFavoriteById(channel.id)
        } else {
            tvDao.insertFavorite(
                FavoriteChannelEntity(
                    channelId = channel.id,
                    name = channel.name,
                    logoUrl = channel.logoUrl,
                    category = channel.category,
                    streamUrl = channel.streamUrl
                )
            )
        }
    }

    suspend fun recordRecent(channel: Channel) = withContext(Dispatchers.IO) {
        tvDao.insertRecent(
            RecentChannelEntity(
                channelId = channel.id,
                name = channel.name,
                logoUrl = channel.logoUrl,
                category = channel.category,
                streamUrl = channel.streamUrl,
                watchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearRecent() = withContext(Dispatchers.IO) {
        tvDao.clearRecent()
    }

    private fun getFallbackChannels(): List<Channel> {
        return listOf(
            Channel(
                id = "ch_dw_english",
                name = "DW English HD",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/7/75/Deutsche_Welle_logo.svg",
                category = "News",
                rawCategory = "News",
                streamUrl = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
                resolution = "720P",
                countryCode = "DE",
                countryName = "Germany"
            ),
            Channel(
                id = "ch_redbull_tv",
                name = "Red Bull TV",
                logoUrl = "https://i.imgur.com/w8qP4sJ.png",
                category = "Sports",
                rawCategory = "Sports",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                resolution = "1080P",
                countryCode = "AT",
                countryName = "Austria"
            ),
            Channel(
                id = "ch_nasa_tv",
                name = "NASA TV Public HD",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e5/NASA_logo.svg",
                category = "Science",
                rawCategory = "Science",
                streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
                resolution = "1080P",
                countryCode = "US",
                countryName = "United States"
            ),
            Channel(
                id = "ch_pluto_spotlight",
                name = "Pluto TV Spotlight",
                logoUrl = "https://images.pluto.tv/channels/5d0901e18bfdae1f81cf87b2/colorLogoPNG.png",
                category = "Entertainment",
                rawCategory = "Entertainment",
                streamUrl = "https://service-stitcher.clusters.pluto.tv/stitch/hls/channel/5d0901e18bfdae1f81cf87b2/master.m3u8?advertisingId=&appName=web&appVersion=unknown&appStoreUrl=&architecture=&buildVersion=&clientDeviceType=0&deviceDNT=0&deviceId=unknown&deviceMake=unknown&deviceModel=unknown&deviceType=unknown&deviceVersion=unknown&includeExtendedEvents=false&sid=unknown",
                resolution = "720P",
                countryCode = "US",
                countryName = "United States"
            ),
            Channel(
                id = "ch_mux_stream",
                name = "Mux Broadcast Test",
                logoUrl = "https://image.mux.com/x36xhzz/thumbnail.png",
                category = "General",
                rawCategory = "General",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                resolution = "1080P",
                countryCode = "INT",
                countryName = "International"
            )
        )
    }
}
