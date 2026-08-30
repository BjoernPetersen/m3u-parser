package net.bjoernpetersen.m3u

import net.bjoernpetersen.m3u.model.MediaUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI

class GitHubIssueTests {
    @Test
    fun testIssue115Link() {
        val entries = M3uParser.parse("http://stream.webradio.bz:8000/vivalaradio3.m3u")
        assertEquals(1, entries.size)

        assertEquals(
            MediaUrl(URI.create("http://stream.webradio.bz:8000/vivalaradio3.m3u").toURL()),
            entries[0].location,
        )
    }

    @Test
    fun testIssue238Link() {
        val entries = M3uParser.parse(
            javaClass.getResourceAsStream("issue-238-title-comma.m3u").reader(),
        )
        assertEquals(1, entries.size)

        assertEquals(
            "Pink Floyd - Another Brick In The Wall, Pt. 2 - 2011 Remastered Version",
            entries[0].title,
        )
    }
}
