package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.CountryHelper
import com.example.model.Channel
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.LiveRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TvNeonCyan
import com.example.ui.theme.TvRed

@Composable
fun ChannelGridItem(
    channel: Channel,
    isPlaying: Boolean,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isPlaying) 2.dp else 1.dp,
                color = if (isPlaying) TvRed else DarkSurfaceBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onChannelClick(channel) }
            .testTag("channel_card_${channel.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Thumbnail / Logo Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E2230), Color(0xFF12141D))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(channel.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${channel.name} logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                } else {
                    // Fallback TV logo badge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = TvNeonCyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = channel.name.take(3).uppercase(),
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Currently Playing Overlay Badge
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvRed)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "Now Playing",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PLAYING",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Resolution Badge if available
                if (channel.resolution != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = channel.resolution,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvNeonCyan
                        )
                    }
                }
            }

            // Channel Details Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = if (isPlaying) TvRed else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (channel.countryCode != null) {
                            Text(
                                text = CountryHelper.getFlagEmoji(channel.countryCode),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (channel.countryCode != null) "${channel.category} • ${channel.countryCode}" else channel.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = { onFavoriteToggle(channel) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("favorite_button_${channel.id}")
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (channel.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (channel.isFavorite) LiveRed else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelListItem(
    channel: Channel,
    isPlaying: Boolean,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPlaying) Color(0xFF1E1419) else DarkSurfaceElevated)
            .border(
                width = if (isPlaying) 1.5.dp else 1.dp,
                color = if (isPlaying) TvRed else DarkSurfaceBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onChannelClick(channel) }
            .padding(10.dp)
            .testTag("channel_list_item_${channel.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141722)),
            contentAlignment = Alignment.Center
        ) {
            if (!channel.logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(channel.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(26.dp)
                )
            }

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Playing",
                        tint = TvRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isPlaying) TvRed else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (channel.resolution != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkSurfaceBorder)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = channel.resolution,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvNeonCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(LiveRed)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = channel.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                if (channel.countryCode != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = CountryHelper.getFlagEmoji(channel.countryCode),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = channel.countryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Action Favorite Button
        IconButton(
            onClick = { onFavoriteToggle(channel) },
            modifier = Modifier.testTag("favorite_button_${channel.id}")
        ) {
            Icon(
                imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (channel.isFavorite) "Favorite" else "Not favorite",
                tint = if (channel.isFavorite) LiveRed else TextMuted
            )
        }
    }
}
