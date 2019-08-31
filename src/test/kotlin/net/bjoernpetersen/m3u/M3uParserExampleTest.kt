package net.bjoernpetersen.m3u

import net.bjoernpetersen.m3u.model.M3uEntry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

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

    @TestFactory
    fun testWikiSimple(): List<DynamicTest> {
        return listOf(
            "wiki_simple.m3u",
            "wiki_simple_comments.m3u"
        ).map { name ->
            dynamicTest(name) {
                assertThat(M3uParser.parse(javaClass.getResourceAsStream(name).reader()))
                    .hasSize(7)
                    .allMatch { it.duration == null && it.title == null }
                    .matches { list ->
                        val titles = list.asSequence().map { it.location }.distinct().count()
                        titles == list.size
                    }
            }
        }
    }

    @TestFactory
    fun testWikiExtended(): List<DynamicTest> {
        return listOf(
            "wiki_extended.m3u",
            "wiki_extended_missing_header.m3u",
            "wiki_extended_comments.m3u"
        ).map { name ->
            dynamicTest(name) {
                assertThat(M3uParser.parse(javaClass.getResourceAsStream(name).reader()))
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
    }

    @TestFactory
    fun testWikiMixed(): List<DynamicTest> {
        return listOf(
            "wiki_mixed.m3u",
            "wiki_mixed_empty_lines.m3u"
        ).map { name ->
            dynamicTest(name) {
                val entries = M3uParser.parse(javaClass.getResourceAsStream(name).reader())
                assertEquals(7, entries.size)

                val simple = listOf(entries[1], entries[4])
                simple.forEach {
                    assertThat(it)
                        .returns(null, M3uEntry::duration)
                        .returns(null, M3uEntry::title)
                }

                val extended = entries.filterNot { it in simple }
                extended.forEach { entry ->
                    assertThat(entry)
                        .matches { it.duration != null }
                        .matches { it.title != null }
                }
            }
        }
    }
}
