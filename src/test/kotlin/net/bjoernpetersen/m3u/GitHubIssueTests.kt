package net.bjoernpetersen.m3u

import net.bjoernpetersen.m3u.model.MediaUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URL

class GitHubIssueTests {
    @Test
    fun testIssue115Link() {
        val entries = M3uParser.parse("http://stream.webradio.bz:8000/vivalaradio3.m3u")
        assertEquals(1, entries.size)

        assertEquals(
            MediaUrl(URL("http://stream.webradio.bz:8000/vivalaradio3.m3u")),
            entries[0].location,
        )
    }
}
