package net.bjoernpetersen.m3u

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.LinkedList
import kotlin.streams.asSequence
import mu.KotlinLogging
import net.bjoernpetersen.m3u.model.M3uEntry
import net.bjoernpetersen.m3u.model.MediaLocation
import net.bjoernpetersen.m3u.model.MediaPath

/**
 * Can be used to parse `.m3u` files.
 *
 * Accepts several input formats:
 *
 * - a file [path][Path]
 * - an [InputStreamReader]
 * - a string containing the content of an `.m3u` file
 */
object M3uParser {
    private const val COMMENT_START = '#'
    private const val EXTENDED_HEADER = "${COMMENT_START}EXTM3U"
    private const val SECONDS = "seconds"
    private const val TITLE = "title"
    private const val EXTENDED_INFO =
        """${COMMENT_START}EXTINF:(?<$SECONDS>[-]?\d+).*,(?<$TITLE>.+)"""

    private val logger = KotlinLogging.logger { }

    private val infoRegex = Regex(EXTENDED_INFO)

    /**
     * Parses the specified file.
     *
     * Comment lines and lines which can't be parsed are dropped.
     *
     * @param m3uFile a path to an .m3u file
     * @param charset the file's encoding, defaults to UTF-8
     * @return a list of all contained entries in order
     * @throws IOException if file can't be read
     * @throws IllegalArgumentException if file is not a regular file
     */
    @Throws(IOException::class)
    @JvmStatic
    @JvmOverloads
    fun parse(m3uFile: Path, charset: Charset = Charsets.UTF_8): List<M3uEntry> {
        require(Files.isRegularFile(m3uFile)) { "$m3uFile is not a file" }
        return parse(Files.lines(m3uFile, charset).asSequence(), m3uFile.parent)
    }

    /**
     * Parses the [InputStream] from the specified reader.
     *
     * Comment lines and lines which can't be parsed are dropped.
     *
     * @param m3uContentReader a reader reading the content of an `.m3u` file
     * @param baseDir a base dir for resolving relative paths
     * @return a list of all parsed entries in order
     */
    @JvmStatic
    @JvmOverloads
    fun parse(m3uContentReader: InputStreamReader, baseDir: Path? = null): List<M3uEntry> {
        return m3uContentReader.buffered().useLines { parse(it, baseDir) }
    }

    /**
     * Parses the specified content of a `.m3u` file.
     *
     * Comment lines and lines which can't be parsed are dropped.
     *
     * @param m3uContent the content of a `.m3u` file
     * @param baseDir a base dir for resolving relative paths
     * @return a list of all parsed entries in order
     */
    @JvmStatic
    @JvmOverloads
    fun parse(m3uContent: String, baseDir: Path? = null): List<M3uEntry> {
        return parse(m3uContent.lineSequence(), baseDir)
    }

    /**
     * Recursively resolves all playlist files contained as entries in the given list.
     *
     * Note that unresolvable playlist file entries will be dropped.
     *
     * @param entries a list of playlist entries
     * @param charset the encoding to be used to read nested playlist files, defaults to UTF-8
     */
    @JvmStatic
    @JvmOverloads
    fun resolveNestedPlaylists(
        entries: List<M3uEntry>,
        charset: Charset = Charsets.UTF_8
    ): List<M3uEntry> {
        return resolveRecursively(entries, charset)
    }

    // TODO: fix detekt issues
    @Suppress("NestedBlockDepth", "ReturnCount")
    private fun parse(lines: Sequence<String>, baseDir: Path?): List<M3uEntry> {
        val filtered = lines
            .filterNot { it.isBlank() }
            .map { it.trimEnd() }
            .dropWhile { it == EXTENDED_HEADER }
            .iterator()

        if (!filtered.hasNext()) return emptyList()

        val entries = LinkedList<M3uEntry>()

        var currentLine: String
        var match: MatchResult? = null
        while (filtered.hasNext()) {
            currentLine = filtered.next()

            while (currentLine.startsWith(COMMENT_START)) {
                val newMatch = infoRegex.matchEntire(currentLine)
                if (newMatch != null) {
                    if (match != null) logger.debug { "Ignoring info line: ${match!!.value}" }
                    match = newMatch
                } else logger.debug { "Ignoring comment line $currentLine" }

                if (filtered.hasNext()) currentLine = filtered.next()
                else return entries
            }

            val entry = if (currentLine.startsWith(COMMENT_START)) continue
            else if (match == null) {
                parseSimple(currentLine, baseDir)
            } else {
                parseExtended(match, currentLine, baseDir)
            }

            match = null

            if (entry != null) entries.add(entry)
            else logger.warn("Ignored line $currentLine")
        }

        return entries
    }

    private fun parseSimple(location: String, baseDir: Path?): M3uEntry? {
        return try {
            M3uEntry(MediaLocation(location, baseDir))
        } catch (e: IllegalArgumentException) {
            logger.warn(e) { "Could not parse as location: $location" }
            null
        }
    }

    private fun parseExtended(infoMatch: MatchResult, location: String, baseDir: Path?): M3uEntry? {
        val mediaLocation = try {
            MediaLocation(location, baseDir)
        } catch (e: IllegalArgumentException) {
            logger.warn(e) { "Could not parse as location: $location" }
            return null
        }

        val duration = infoMatch.groups[SECONDS]?.value?.toLong()
            ?.let { if (it < 0) null else it }
            ?.let { Duration.ofSeconds(it) }
        val title = infoMatch.groups[TITLE]?.value
        return M3uEntry(mediaLocation, duration, title)
    }

    private fun resolveRecursively(
        source: List<M3uEntry>,
        charset: Charset,
        result: MutableList<M3uEntry> = LinkedList()
    ): List<M3uEntry> {
        for (entry in source) {
            val location = entry.location
            if (location is MediaPath && location.isPlaylistPath) {
                resolveNestedPlaylist(location.path, charset, result)
            } else {
                result.add(entry)
            }
        }
        return result
    }

    private fun resolveNestedPlaylist(
        path: Path,
        charset: Charset,
        result: MutableList<M3uEntry>
    ) {
        if (!Files.isRegularFile(path)) {
            return
        }

        val parsed = try {
            parse(path, charset)
        } catch (e: IOException) {
            logger.warn(e) { "Could not parse nested playlist file: $path" }
            return
        }

        resolveRecursively(parsed, charset, result)
    }
}
