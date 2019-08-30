package net.bjoernpetersen.m3u

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class M3uParserExampleTest {
    @Test
    fun testCleanRadio() {
        assertThat(M3uParser.parse(javaClass.getResourceAsStream("clean_radio_de.m3u").reader()))
            .hasSize(131)
            .allMatch { it.duration == null && !it.title.isNullOrBlank() }
            .matches { list ->
                val titles = list.asSequence().map { it.title }.distinct().count()
                titles == list.size
            }
            .matches { list ->
                val titles = list.asSequence().map { it.location }.distinct().count()
                titles == list.size
            }
    }

    @Test
    fun testWikiSimple() {
        assertThat(M3uParser.parse(javaClass.getResourceAsStream("wiki_simple.m3u").reader()))
            .hasSize(7)
            .allMatch { it.duration == null && it.title == null }
            .matches { list ->
                val titles = list.asSequence().map { it.location }.distinct().count()
                titles == list.size
            }
    }

    @Test
    fun testWikiExtended() {
        assertThat(M3uParser.parse(javaClass.getResourceAsStream("wiki_extended.m3u").reader()))
            .hasSize(7)
            .allMatch { it.duration != null && !it.title.isNullOrBlank() }
            .matches { list ->
                val titles = list.asSequence().map { it.duration }.distinct().count()
                titles == list.size
            }
            .matches { list ->
                val titles = list.asSequence().map { it.title }.distinct().count()
                titles == list.size
            }
            .matches { list ->
                val titles = list.asSequence().map { it.location }.distinct().count()
                titles == list.size
            }
    }
}
