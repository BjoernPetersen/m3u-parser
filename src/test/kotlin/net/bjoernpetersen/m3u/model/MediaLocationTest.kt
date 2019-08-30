package net.bjoernpetersen.m3u.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URL
import java.nio.file.Paths

@Disabled
class MediaLocationTest {
    @Test
    fun `relative path`() {
        val path = TEST_FILE
        assertThat(MediaLocation(path))
            .asInstanceOf(InstanceOfAssertFactories.type(MediaPath::class.java))
            .returns(path) { it.path.toString() }
            .returns(Paths.get(path).toUri().toURL()) { it.url }
    }

    @Test
    fun `relative path with dir`() {
        val path = TEST_FILE
        val dir = Paths.get(TEST_DIR)
        val resolvedPath = dir.resolve(path)
        assertThat(MediaLocation(path, dir))
            .asInstanceOf(InstanceOfAssertFactories.type(MediaPath::class.java))
            .returns(resolvedPath) { it.path }
            .returns(resolvedPath.toUri().toURL()) { it.url }
    }

    @Test
    fun `file URL`() {
        val urlString = "file:///$TEST_FILE"
        val url = URL(urlString)
        val path = Paths.get(url.toURI())
        assertThat(MediaLocation(urlString))
            .asInstanceOf(InstanceOfAssertFactories.type(MediaPath::class.java))
            .returns(path.toUri().toURL().toString()) { it.url.toExternalForm() }
            .returns(path) { it.path }
    }

    @Test
    fun `http URL`() {
        val url = TEST_REMOTE_URL
        assertThat(MediaLocation(url))
            .asInstanceOf(InstanceOfAssertFactories.type(MediaUrl::class.java))
            .returns(url) { it.url.toExternalForm() }
    }

    @Test
    fun invalid() {
        val location = "path:///test.mp3"
        assertThrows<IllegalArgumentException> {
            MediaLocation(location)
        }
    }

    private companion object {
        const val TEST_FILE = "test.mp3"
        const val TEST_DIR = "testdir"
        const val TEST_REMOTE_URL = "http://www.example.com/musik/titel4.mp3"
    }
}
