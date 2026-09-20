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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CountryItem
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TvNeonCyan

@Composable
fun CountryChipRow(
    countries: List<CountryItem>,
    selectedCountry: String,
    onCountrySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading indicator label/icon
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = "Region Filter",
                    tint = TvNeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Region:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TvNeonCyan,
                    fontSize = 11.sp
                )
            }
        }

        items(countries, key = { it.code }) { country ->
            val isSelected = country.code.equals(selectedCountry, ignoreCase = true)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) TvNeonCyan.copy(alpha = 0.2f) else DarkSurfaceElevated
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) TvNeonCyan else DarkSurfaceBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onCountrySelected(country.code) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("country_chip_${country.code.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = country.flagEmoji,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = if (country.code == "ALL") "All Regions" else if (country.name.length > 14) country.code else country.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        ),
                        color = if (isSelected) TvNeonCyan else TextPrimary
                    )

                    if (country.count > 0) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) TvNeonCyan.copy(alpha = 0.3f)
                                    else DarkSurfaceBorder
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (country.count > 999) "${country.count / 1000}k" else "${country.count}",
                                fontSize = 9.sp,
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
