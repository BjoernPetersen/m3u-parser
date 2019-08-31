package net.bjoernpetersen.m3u.model

import java.time.Duration

/**
 * An entry in a `.m3u` file.
 *
 * @param location the location of the file
 * @param duration the media item's duration, or null
 * @param title the media item's title, or null
 */
data class M3uEntry @JvmOverloads constructor(
    val location: MediaLocation,
    val duration: Duration? = null,
    val title: String? = null
)
