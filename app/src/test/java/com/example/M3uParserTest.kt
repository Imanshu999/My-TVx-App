package com.example

import com.example.data.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream

class M3uParserTest {

    @Test
    fun testM3uParsing() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="CNN.us" tvg-logo="https://example.com/cnn.png" group-title="News",CNN News (1080p)
            https://example.com/cnn/live.m3u8
            #EXTINF:-1 tvg-id="ESPN.us" tvg-logo="https://example.com/espn.png" group-title="Sports",ESPN Sports HD
            #EXTVLCOPT:http-user-agent=TestAgent
            http://example.com/espn/stream.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(ByteArrayInputStream(m3uContent.toByteArray()))
        assertEquals(2, channels.size)

        val ch1 = channels[0]
        assertEquals("CNN News (1080p)", ch1.name)
        assertEquals("https://example.com/cnn.png", ch1.logoUrl)
        assertEquals("News", ch1.category)
        assertEquals("https://example.com/cnn/live.m3u8", ch1.streamUrl)
        assertEquals("1080P", ch1.resolution)
        assertEquals("US", ch1.countryCode)
        assertEquals("United States", ch1.countryName)

        val ch2 = channels[1]
        assertEquals("ESPN Sports HD", ch2.name)
        assertEquals("https://example.com/espn.png", ch2.logoUrl)
        assertEquals("Sports", ch2.category)
        assertEquals("http://example.com/espn/stream.m3u8", ch2.streamUrl)
        assertEquals("HD", ch2.resolution)
        assertEquals("US", ch2.countryCode)
        assertEquals("United States", ch2.countryName)
    }
}
