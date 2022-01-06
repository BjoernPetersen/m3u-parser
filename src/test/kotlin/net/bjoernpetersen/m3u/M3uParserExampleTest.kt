package net.bjoernpetersen.m3u

import net.bjoernpetersen.m3u.model.M3uEntry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.nio.file.Files
import java.nio.file.Paths

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
            "wiki_extended_comments.m3u",
            "wiki_extended_duplicate_info_line.m3u"
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

    @Test
    fun testIptv() {
        val parsed = M3uParser.parse(javaClass.getResourceAsStream("iptv_es.m3u").reader())
        assertThat(parsed)
            .hasSize(260)
            .allMatch { it.duration == null }
            .allMatch { it.title != null }
            .allMatch { it.title!!.isNotBlank() }

        val first = parsed.first()
        assertEquals(
            "https://hlsliveamdgl0-lh.akamaihd.net/i/hlslive_1@586402/master.m3u8",
            first.location.url.toString(),
        )
        assertEquals("Plus24.es", first.metadata["tvg-id"])
        assertEquals("ES", first.metadata["tvg-country"])
        assertEquals("Spanish", first.metadata["tvg-language"])
        assertNull(first.metadata["tvg-logo"])
        assertNull(first.metadata.logo)

        val withLogo = parsed[10]
        assertEquals("https://i.imgur.com/CnIVW9o.jpg", withLogo.metadata.logo)
    }

    @Test
    fun testRecursiveResolution() {
        val files = listOf("rec_1.m3u", "rec_2.m3u", "rec_3.m3u")
        exportFiles(files)
        try {
            val initial = M3uParser.parse(Paths.get(FILE_DIR, "rec_1.m3u"))
            assertThat(initial)
                .hasSize(4)

            assertThat(M3uParser.resolveNestedPlaylists(initial))
                .isNotSameAs(initial)
                .hasSize(6)
                .matches { list ->
                    val locations = list.asSequence().map { it.location }.distinct().count()
                    locations == list.size
                }
        } finally {
            deleteFiles(files)
        }
    }

    private fun deleteFiles(paths: List<String>) {
        paths.map { Paths.get(FILE_DIR, it) }.forEach {
            Files.deleteIfExists(it)
        }
    }

    private fun exportFiles(paths: List<String>) {
        val parent = Paths.get(FILE_DIR)
        if (!Files.isDirectory(parent)) {
            Files.createDirectories(parent)
        }
        for (name in paths) {
            val path = parent.resolve(name)
            javaClass.getResourceAsStream(name).reader().use { reader ->
                Files.newBufferedWriter(path, Charsets.UTF_8).use { writer ->
                    reader.copyTo(writer)
                }
            }
        }
    }

    companion object {
        const val FILE_DIR = "build/tmp/m3us"
    }
}
