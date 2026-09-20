package com.example.data

import com.example.model.Channel
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

object M3uParser {

    private val LOGO_PATTERN = Pattern.compile("tvg-logo=\"([^\"]*)\"")
    private val GROUP_PATTERN = Pattern.compile("group-title=\"([^\"]*)\"")
    private val NAME_PATTERN = Pattern.compile("tvg-name=\"([^\"]*)\"")
    private val TVG_ID_PATTERN = Pattern.compile("tvg-id=\"([^\"]*)\"")
    private val TVG_COUNTRY_PATTERN = Pattern.compile("tvg-country=\"([^\"]*)\"")
    private val RESOLUTION_PATTERN = Pattern.compile("\\b(\\d{3,4}p|4K|UHD|FHD|HD|SD)\\b", Pattern.CASE_INSENSITIVE)
    // Matches patterns like "Channel.us@SD", "BBC.uk", "Name.ca@HD"
    private val TVG_ID_COUNTRY_PATTERN = Pattern.compile("\\.([a-zA-Z]{2})(?:@[\\w]+)?$")

    /**
     * Parses an M3U stream into a list of Channel objects efficiently.
     */
    fun parse(inputStream: InputStream): List<Channel> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val channels = ArrayList<Channel>(4000)

        var currentExtInf: String? = null
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            val trimmed = line?.trim() ?: continue
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                currentExtInf = trimmed
            } else if (!trimmed.startsWith("#") && (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true))) {
                if (currentExtInf != null) {
                    val channel = parseChannel(currentExtInf, trimmed)
                    if (channel != null) {
                        channels.add(channel)
                    }
                    currentExtInf = null
                }
            }
        }

        return channels
    }

    private fun parseChannel(extInf: String, streamUrl: String): Channel? {
        val commaIndex = extInf.lastIndexOf(',')
        var channelName = if (commaIndex != -1 && commaIndex < extInf.length - 1) {
            extInf.substring(commaIndex + 1).trim()
        } else {
            ""
        }

        val logoMatcher = LOGO_PATTERN.matcher(extInf)
        val logoUrl = if (logoMatcher.find()) {
            val raw = logoMatcher.group(1)?.trim()
            if (raw.isNullOrEmpty()) null else raw
        } else {
            null
        }

        val groupMatcher = GROUP_PATTERN.matcher(extInf)
        val rawCategory = if (groupMatcher.find()) {
            val raw = groupMatcher.group(1)?.trim()
            if (raw.isNullOrEmpty()) "General" else raw
        } else {
            "General"
        }

        if (channelName.isEmpty()) {
            val nameMatcher = NAME_PATTERN.matcher(extInf)
            if (nameMatcher.find()) {
                channelName = nameMatcher.group(1)?.trim() ?: ""
            }
        }

        if (channelName.isEmpty()) {
            channelName = "Channel ${streamUrl.hashCode().toString().takeLast(4)}"
        }

        val normalizedCategory = normalizeCategory(rawCategory)

        val resMatcher = RESOLUTION_PATTERN.matcher(channelName)
        val resolution = if (resMatcher.find()) resMatcher.group(1)?.uppercase() else null

        // Detect country code:
        // 1. Explicit tvg-country tag
        // 2. Extracted from tvg-id (e.g. "CNN.us@SD" -> "US")
        // 3. Extracted from channelName suffixes or brackets (e.g. "(US)", "[UK]")
        var detectedCountryCode: String? = null
        val tvgCountryMatcher = TVG_COUNTRY_PATTERN.matcher(extInf)
        if (tvgCountryMatcher.find()) {
            val rawCode = tvgCountryMatcher.group(1)?.trim()?.uppercase()
            if (!rawCode.isNullOrEmpty() && rawCode.length == 2) {
                detectedCountryCode = rawCode
            }
        }

        if (detectedCountryCode == null) {
            val tvgIdMatcher = TVG_ID_PATTERN.matcher(extInf)
            if (tvgIdMatcher.find()) {
                val tvgId = tvgIdMatcher.group(1) ?: ""
                val idCountryMatcher = TVG_ID_COUNTRY_PATTERN.matcher(tvgId)
                if (idCountryMatcher.find()) {
                    detectedCountryCode = idCountryMatcher.group(1)?.uppercase()
                }
            }
        }

        if (detectedCountryCode == null) {
            // Infer from common channel name tags like (US), (UK), (FR), (DE), [US], etc.
            val bracketMatcher = Pattern.compile("[\\[\\(]([a-zA-Z]{2})[\\]\\)]").matcher(channelName)
            if (bracketMatcher.find()) {
                val candidate = bracketMatcher.group(1)?.uppercase()
                if (candidate != null && candidate != "HD" && candidate != "SD" && candidate != "4K") {
                    detectedCountryCode = candidate
                }
            }
        }

        val countryName = if (detectedCountryCode != null) {
            CountryHelper.getCountryName(detectedCountryCode)
        } else {
            "International"
        }

        val id = "ch_${(channelName + streamUrl).hashCode().toString().replace("-", "n")}"

        return Channel(
            id = id,
            name = channelName,
            logoUrl = logoUrl,
            category = normalizedCategory,
            rawCategory = rawCategory,
            streamUrl = streamUrl,
            resolution = resolution,
            countryCode = detectedCountryCode,
            countryName = countryName
        )
    }

    private fun normalizeCategory(raw: String): String {
        val primary = raw.split(';').firstOrNull()?.trim() ?: "General"
        val lower = primary.lowercase()

        return when {
            lower.contains("news") -> "News"
            lower.contains("sport") -> "Sports"
            lower.contains("movie") || lower.contains("cinema") || lower.contains("film") -> "Movies"
            lower.contains("music") -> "Music"
            lower.contains("kid") || lower.contains("child") || lower.contains("cartoon") -> "Kids"
            lower.contains("anim") -> "Animation"
            lower.contains("entertain") || lower.contains("series") -> "Entertainment"
            lower.contains("docu") || lower.contains("nature") || lower.contains("science") -> "Documentary"
            lower.contains("comedy") -> "Comedy"
            lower.contains("relig") -> "Religious"
            lower.contains("shop") -> "Shopping"
            lower.contains("weather") -> "Weather"
            lower.contains("travel") -> "Travel"
            lower.contains("classic") -> "Classic"
            lower.contains("family") -> "Family"
            lower.contains("educat") -> "Education"
            primary.length > 20 -> primary.take(20).trim()
            else -> primary.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
