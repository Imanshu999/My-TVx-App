package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CategoryItem
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TvNeonCyan
import com.example.ui.theme.TvRed

@Composable
fun CategoryChipRow(
    categories: List<CategoryItem>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(categories, key = { it.name }) { category ->
            val isSelected = category.name.equals(selectedCategory, ignoreCase = true)

            val icon = getCategoryIcon(category.name)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) TvRed else DarkSurfaceElevated
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) TvRed else DarkSurfaceBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onCategorySelected(category.name) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag("category_chip_${category.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = category.name,
                            tint = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = if (isSelected) Color.White else TextPrimary
                    )

                    if (category.count > 0 && category.name != "Favorites" && category.name != "Recent") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.25f)
                                    else DarkSurfaceBorder
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (category.count > 999) "${category.count / 1000}k" else "${category.count}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(name: String): ImageVector? {
    return when (name.lowercase()) {
        "all" -> Icons.Default.Widgets
        "favorites" -> Icons.Default.Favorite
        "recent" -> Icons.Default.History
        "news" -> Icons.Default.Newspaper
        "sports" -> Icons.Default.SportsSoccer
        "movies", "cinema" -> Icons.Default.Movie
        "music" -> Icons.Default.MusicNote
        "kids", "children", "animation" -> Icons.Default.ChildCare
        "general" -> Icons.Default.Tv
        else -> null
    }
}
