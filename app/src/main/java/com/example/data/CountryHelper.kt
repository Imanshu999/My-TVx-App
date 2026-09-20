package com.example.data

import java.util.Locale

object CountryHelper {

    private val CODE_TO_NAME = mapOf(
        "US" to "United States",
        "GB" to "United Kingdom",
        "UK" to "United Kingdom",
        "CA" to "Canada",
        "AU" to "Australia",
        "DE" to "Germany",
        "FR" to "France",
        "IT" to "Italy",
        "ES" to "Spain",
        "UA" to "Ukraine",
        "RU" to "Russia",
        "IN" to "India",
        "BR" to "Brazil",
        "MX" to "Mexico",
        "JP" to "Japan",
        "KR" to "South Korea",
        "CN" to "China",
        "NL" to "Netherlands",
        "BE" to "Belgium",
        "CH" to "Switzerland",
        "AT" to "Austria",
        "SE" to "Sweden",
        "NO" to "Norway",
        "DK" to "Denmark",
        "FI" to "Finland",
        "PL" to "Poland",
        "CZ" to "Czech Republic",
        "TR" to "Turkey",
        "GR" to "Greece",
        "PT" to "Portugal",
        "AR" to "Argentina",
        "CL" to "Chile",
        "CO" to "Colombia",
        "PE" to "Peru",
        "ZA" to "South Africa",
        "EG" to "Egypt",
        "SA" to "Saudi Arabia",
        "AE" to "UAE",
        "IL" to "Israel",
        "GE" to "Georgia",
        "MA" to "Morocco",
        "ID" to "Indonesia",
        "MY" to "Malaysia",
        "PH" to "Philippines",
        "TH" to "Thailand",
        "VN" to "Vietnam",
        "PK" to "Pakistan",
        "BD" to "Bangladesh",
        "NG" to "Nigeria",
        "KE" to "Kenya",
        "GH" to "Ghana",
        "NZ" to "New Zealand",
        "IE" to "Ireland",
        "RO" to "Romania",
        "HU" to "Hungary",
        "BG" to "Bulgaria",
        "RS" to "Serbia",
        "HR" to "Croatia",
        "SK" to "Slovakia",
        "SI" to "Slovenia"
    )

    fun getCountryName(code: String): String {
        val upper = code.uppercase().trim()
        val mapped = CODE_TO_NAME[upper]
        if (mapped != null) return mapped

        return try {
            val loc = Locale.Builder().setRegion(upper).build()
            val display = loc.displayCountry
            if (display.isNotEmpty() && !display.equals(upper, ignoreCase = true)) {
                display
            } else {
                upper
            }
        } catch (_: Exception) {
            upper
        }
    }

    /**
     * Converts a 2-letter ISO country code into the Unicode Regional Indicator Symbol emoji flag.
     * e.g., "US" -> 🇺🇸, "GB" -> 🇬🇧
     */
    fun getFlagEmoji(code: String?): String {
        if (code == null || code.length != 2) return "🌐"
        val upper = code.uppercase()
        val firstChar = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
        if (firstChar !in 0x1F1E6..0x1F1FF || secondChar !in 0x1F1E6..0x1F1FF) return "🌐"
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }
}
