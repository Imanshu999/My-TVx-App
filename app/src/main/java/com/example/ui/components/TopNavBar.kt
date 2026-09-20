package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun TopNavBar(
    isSearching: Boolean,
    searchQuery: String,
    isGridView: Boolean,
    isLoading: Boolean,
    totalChannels: Int,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onViewModeToggle: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (!isSearching) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand Title & Live TV Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("brand_header")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TvRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "My TVx Logo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "My ",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "TVx",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = TvRed
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Live Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LiveRed)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }

                        if (totalChannels > 0) {
                            Text(
                                text = "$totalChannels channels available",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Action buttons: Search, Grid/List toggle, Refresh
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSearchToggle,
                        modifier = Modifier.testTag("search_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Channels",
                            tint = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onViewModeToggle,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (!isGridView) DarkSurfaceBorder else Color.Transparent)
                            .testTag("view_mode_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                            tint = if (!isGridView) TvNeonCyan else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = TvRed,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Playlist",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }
        } else {
            // Search Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSearchToggle,
                    modifier = Modifier.testTag("search_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close Search",
                        tint = TextPrimary
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_text_input"),
                    placeholder = {
                        Text("Search channels by name or category...", color = TextMuted, fontSize = 14.sp)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = TvRed,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = TvRed
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                    })
                )
            }
        }
    }
}
